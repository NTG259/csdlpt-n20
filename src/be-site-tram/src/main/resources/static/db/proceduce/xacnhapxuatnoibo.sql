USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_XacNhanNhap_NoiBo]    Script Date: 02/06/2026 21:19:15 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER   PROCEDURE [dbo].[sp_XacNhanNhap_NoiBo]
    @MaPhieuNhap UNIQUEIDENTIFIER,
    @MaNhanVienNhap UNIQUEIDENTIFIER
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE
        @MaDonHang UNIQUEIDENTIFIER,
        @MaKhoXuat VARCHAR(20),
        @MaKhoNhap VARCHAR(20),
        @TrangThaiNhap VARCHAR(30),
        @LaKhoXuatLocal BIT = 0,
        @ErrMsg NVARCHAR(4000),
        @ErrSeverity INT,
        @ErrState INT;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- =====================================================
        -- 1. Lấy thông tin phiếu nhập
        -- =====================================================
        SELECT
            @MaDonHang = MaDonHang,
            @MaKhoXuat = MaKhoXuat,
            @MaKhoNhap = MaKhoNhap,
            @TrangThaiNhap = TrangThaiNhap
        FROM PhieuNhapKho WITH (UPDLOCK, HOLDLOCK)
        WHERE MaPhieuNhap = @MaPhieuNhap;

        IF @MaKhoNhap IS NULL
        BEGIN
            RAISERROR(N'Không tìm thấy phiếu nhập kho.', 16, 1);
        END;

        IF @MaDonHang IS NULL
        BEGIN
            RAISERROR(N'Phiếu nhập không có mã đơn hàng, không thể đối chiếu phiếu xuất nguồn.', 16, 1);
        END;

        IF @MaKhoXuat IS NULL
        BEGIN
            RAISERROR(N'Phiếu nhập không có mã kho xuất.', 16, 1);
        END;

        IF @TrangThaiNhap <> 'waiting_import'
        BEGIN
            RAISERROR(N'Chỉ được xác nhận phiếu nhập đang ở trạng thái waiting_import.', 16, 1);
        END;

        IF NOT EXISTS (
            SELECT 1
            FROM NguoiDung
            WHERE MaND = @MaNhanVienNhap
              AND TrangThai = 1
        )
        BEGIN
            RAISERROR(N'Nhân viên nhập kho không hợp lệ.', 16, 1);
        END;

        -- =====================================================
        -- 2. Kiểm tra phiếu nhập có chi tiết không
        -- =====================================================
        IF NOT EXISTS (
            SELECT 1
            FROM ChiTietPhieuNhap
            WHERE MaPhieuNhap = @MaPhieuNhap
        )
        BEGIN
            RAISERROR(N'Phiếu nhập không có chi tiết sản phẩm.', 16, 1);
        END;

        -- =====================================================
        -- 3. Xác định kho xuất là local hay remote
        -- Nếu MaKhoXuat tồn tại trong bảng Kho local
        -- => phiếu xuất nằm ở site hiện tại.
        -- Nếu không tồn tại
        -- => coi là kho remote, ví dụ SITE_NAM.
        -- =====================================================
        IF EXISTS (
            SELECT 1
            FROM Kho
            WHERE MaKho = @MaKhoXuat
        )
        BEGIN
            SET @LaKhoXuatLocal = 1;
        END;
        ELSE
        BEGIN
            SET @LaKhoXuatLocal = 0;
        END;

        -- =====================================================
        -- 4. Kiểm tra phiếu xuất nguồn đã exported chưa
        -- =====================================================
        IF @LaKhoXuatLocal = 1
        BEGIN
            -- Ví dụ KB02 -> KB01
            -- Phiếu xuất nằm ở site Bắc.
            IF NOT EXISTS (
                SELECT 1
                FROM PhieuXuatKho WITH (UPDLOCK, HOLDLOCK)
                WHERE MaDonHang = @MaDonHang
                  AND MaKhoXuat = @MaKhoXuat
                  AND MaKhoNhan = @MaKhoNhap
                  AND TrangThaiXuat = 'exported'
            )
            BEGIN
                RAISERROR(N'Phiếu xuất nội bộ local chưa exported, chưa thể xác nhận nhập.', 16, 1);
            END;
        END
        ELSE
        BEGIN
            -- Ví dụ KN01 -> KB01
            -- Phiếu xuất nằm ở SITE_NAM.
            IF NOT EXISTS (
                SELECT 1
                FROM [LINK].[store_management].dbo.PhieuXuatKho
                WHERE MaDonHang = @MaDonHang
                  AND MaKhoXuat = @MaKhoXuat
                  AND MaKhoNhan = @MaKhoNhap
                  AND TrangThaiXuat = 'exported'
            )
            BEGIN
                RAISERROR(N'Phiếu xuất nội bộ remote chưa exported, chưa thể xác nhận nhập.', 16, 1);
            END;
        END;

        -- =====================================================
        -- 5. Gom số lượng nhập theo sản phẩm
        -- =====================================================
        CREATE TABLE #TongNhap
        (
            MaSP VARCHAR(20) PRIMARY KEY,
            SoLuongNhap INT NOT NULL
        );

        INSERT INTO #TongNhap(MaSP, SoLuongNhap)
        SELECT
            MaSP,
            SUM(SoLuong)
        FROM ChiTietPhieuNhap
        WHERE MaPhieuNhap = @MaPhieuNhap
        GROUP BY MaSP;

        -- =====================================================
        -- 6. Cộng tồn kho nhận
        -- Lưu ý:
        -- SoLuongTon += x
        -- SoLuongDatHang += x
        -- Vì hàng về KB01 nhưng vẫn bị giữ cho đơn hàng này.
        -- =====================================================
        UPDATE tk
        SET
            tk.SoLuongTon = tk.SoLuongTon + tn.SoLuongNhap,
            tk.SoLuongDatHang = tk.SoLuongDatHang + tn.SoLuongNhap,
            tk.NgayCapNhat = SYSDATETIME()
        FROM TonKho tk
        INNER JOIN #TongNhap tn
            ON tn.MaSP = tk.MaSP
        WHERE tk.MaKho = @MaKhoNhap;

        -- Nếu kho nhận chưa từng có sản phẩm này thì thêm mới
        INSERT INTO TonKho
        (
            MaKho,
            MaSP,
            SoLuongTon,
            SoLuongDatHang,
            NgayCapNhat
        )
        SELECT
            @MaKhoNhap,
            tn.MaSP,
            tn.SoLuongNhap,
            tn.SoLuongNhap,
            SYSDATETIME()
        FROM #TongNhap tn
        WHERE NOT EXISTS (
            SELECT 1
            FROM TonKho tk
            WHERE tk.MaKho = @MaKhoNhap
              AND tk.MaSP = tn.MaSP
        );

        -- =====================================================
        -- 7. Cập nhật phiếu nhập
        -- =====================================================
        UPDATE PhieuNhapKho
        SET
            TrangThaiNhap = 'imported',
            MaNhanVienNhap = @MaNhanVienNhap,
            NgayNhap = SYSDATETIME()
        WHERE MaPhieuNhap = @MaPhieuNhap;

        -- =====================================================
        -- 8. Đồng bộ TrangThaiNhan ở phiếu xuất nguồn
        -- Nếu bạn không muốn dùng TrangThaiNhan trong PhieuXuatKho,
        -- có thể bỏ cả đoạn 8 này.
        -- =====================================================
        IF @LaKhoXuatLocal = 1
        BEGIN
            -- KB02 -> KB01
            UPDATE PhieuXuatKho
            SET TrangThaiNhan = 'received'
            WHERE MaDonHang = @MaDonHang
              AND MaKhoXuat = @MaKhoXuat
              AND MaKhoNhan = @MaKhoNhap
              AND TrangThaiXuat = 'exported'
              AND TrangThaiNhan = 'waiting_receive';
        END
        ELSE
        BEGIN
            -- KN01 -> KB01
            UPDATE [LINK].[store_management].dbo.PhieuXuatKho
            SET TrangThaiNhan = 'received'
            WHERE MaDonHang = @MaDonHang
              AND MaKhoXuat = @MaKhoXuat
              AND MaKhoNhan = @MaKhoNhap
              AND TrangThaiXuat = 'exported'
              AND TrangThaiNhan = 'waiting_receive';
        END;

        COMMIT TRANSACTION;

        SELECT
            @MaPhieuNhap AS MaPhieuNhap,
            @MaDonHang AS MaDonHang,
            @MaKhoXuat AS MaKhoXuat,
            @MaKhoNhap AS MaKhoNhap,
            N'Xác nhận nhập nội bộ thành công' AS ThongBao;

    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        SET @ErrMsg = ERROR_MESSAGE();
        SET @ErrSeverity = ERROR_SEVERITY();
        SET @ErrState = ERROR_STATE();

        RAISERROR(@ErrMsg, @ErrSeverity, @ErrState);
    END CATCH;
END;
