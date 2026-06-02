USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_TaoPhieuNhap_NoiBo_Batch]    Script Date: 02/06/2026 20:57:31 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER   PROCEDURE [dbo].[sp_TaoPhieuNhap_NoiBo_Batch]
    @MaDonHang UNIQUEIDENTIFIER,
    @PhanBo dbo.PhanBoXuatType READONLY
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @MaDonHang IS NULL
    BEGIN
        RAISERROR(N'Mã đơn hàng không được NULL.', 16, 1);
        RETURN;
    END;

    IF NOT EXISTS (SELECT 1 FROM @PhanBo)
    BEGIN
        RAISERROR(N'Danh sách phân bổ nhập kho rỗng.', 16, 1);
        RETURN;
    END;

    IF EXISTS (
        SELECT 1
        FROM @PhanBo
        WHERE MaSP IS NULL
           OR MaKhoXuat IS NULL
           OR MaKhoNhan IS NULL
           OR SoLuongXuat IS NULL
           OR SoLuongXuat <= 0
    )
    BEGIN
        RAISERROR(N'Dữ liệu tạo phiếu nhập không hợp lệ.', 16, 1);
        RETURN;
    END;

    DECLARE @StartedTran BIT = 0;
    DECLARE @ErrMsg NVARCHAR(4000), @ErrSeverity INT, @ErrState INT;

    BEGIN TRY
        IF @@TRANCOUNT = 0
        BEGIN
            SET @StartedTran = 1;
            BEGIN TRANSACTION;
        END;

        -- Chỉ nhận vào kho thuộc site hiện tại.
        IF EXISTS (
            SELECT 1
            FROM @PhanBo p
            WHERE NOT EXISTS (
                SELECT 1
                FROM Kho k
                WHERE k.MaKho = p.MaKhoNhan
                  AND k.TrangThai = 1
            )
        )
        BEGIN
            RAISERROR(N'Kho nhận không tồn tại hoặc không hoạt động tại site hiện tại.', 16, 1);
        END;

        CREATE TABLE #NhapNorm
        (
            MaCTDH UNIQUEIDENTIFIER NULL,
            MaSP VARCHAR(20) NOT NULL,
            MaKhoXuat VARCHAR(20) NOT NULL,
            MaKhoNhap VARCHAR(20) NOT NULL,
            SoLuongNhap INT NOT NULL
        );

        INSERT INTO #NhapNorm
        (
            MaCTDH,
            MaSP,
            MaKhoXuat,
            MaKhoNhap,
            SoLuongNhap
        )
        SELECT
            MaCTDH,
            MaSP,
            MaKhoXuat,
            MaKhoNhan,
            SoLuongXuat
        FROM @PhanBo
        WHERE MaKhoNhan IS NOT NULL
          AND MaKhoXuat <> MaKhoNhan;

        IF NOT EXISTS (SELECT 1 FROM #NhapNorm)
        BEGIN
            IF @StartedTran = 1 AND @@TRANCOUNT > 0
                COMMIT TRANSACTION;

            SELECT 
                @MaDonHang AS MaDonHang,
                N'Không có phiếu nhập nội bộ cần tạo' AS ThongBao;
            RETURN;
        END;

        CREATE TABLE #PNKMap
        (
            MaKhoXuat VARCHAR(20) NOT NULL,
            MaKhoNhap VARCHAR(20) NOT NULL,
            MaPhieuNhap UNIQUEIDENTIFIER NOT NULL,
            PRIMARY KEY (MaKhoXuat, MaKhoNhap)
        );

        INSERT INTO #PNKMap
        (
            MaKhoXuat,
            MaKhoNhap,
            MaPhieuNhap
        )
        SELECT
            x.MaKhoXuat,
            x.MaKhoNhap,
            ISNULL(pn.MaPhieuNhap, NEWID()) AS MaPhieuNhap
        FROM (
            SELECT DISTINCT 
                MaKhoXuat,
                MaKhoNhap
            FROM #NhapNorm
        ) x
        LEFT JOIN PhieuNhapKho pn WITH (UPDLOCK, HOLDLOCK)
            ON pn.MaDonHang = @MaDonHang
           AND pn.MaKhoXuat = x.MaKhoXuat
           AND pn.MaKhoNhap = x.MaKhoNhap;

        INSERT INTO PhieuNhapKho
        (
            MaPhieuNhap,
            MaDonHang,
            MaKhoXuat,
            MaKhoNhap,
            NgayNhap,
            MaNhanVienNhap,
            TrangThaiNhap,
            GhiChu
        )
        SELECT
            m.MaPhieuNhap,
            @MaDonHang,
            m.MaKhoXuat,
            m.MaKhoNhap,
            SYSDATETIME(),
            NULL,
            'waiting_import',
            N'Phiếu nhập nội bộ chờ nhận hàng từ kho khác'
        FROM #PNKMap m
        WHERE NOT EXISTS (
            SELECT 1
            FROM PhieuNhapKho pn
            WHERE pn.MaDonHang = @MaDonHang
              AND pn.MaKhoXuat = m.MaKhoXuat
              AND pn.MaKhoNhap = m.MaKhoNhap
        );

        -- =====================================================
        -- SỬA Ở ĐÂY:
        -- Không ghi DonGiaNhap = 0 nữa.
        -- Lấy tạm theo SanPham_Core.GiaBan.
        -- =====================================================
        INSERT INTO ChiTietPhieuNhap
        (
            MaCTPN,
            MaPhieuNhap,
            MaSP,
            SoLuong,
            DonGiaNhap
        )
        SELECT
            NEWID(),
            m.MaPhieuNhap,
            n.MaSP,
            SUM(n.SoLuongNhap) AS SoLuongNhap,
            sp.GiaBan AS DonGiaNhap
        FROM #NhapNorm n
        INNER JOIN #PNKMap m
            ON m.MaKhoXuat = n.MaKhoXuat
           AND m.MaKhoNhap = n.MaKhoNhap
        INNER JOIN SanPham_Core sp
            ON sp.MaSP = n.MaSP
        WHERE NOT EXISTS (
            SELECT 1
            FROM ChiTietPhieuNhap ctpn
            WHERE ctpn.MaPhieuNhap = m.MaPhieuNhap
              AND ctpn.MaSP = n.MaSP
        )
        GROUP BY
            m.MaPhieuNhap,
            n.MaSP,
            sp.GiaBan;

        IF @StartedTran = 1 AND @@TRANCOUNT > 0
            COMMIT TRANSACTION;

        SELECT
            @MaDonHang AS MaDonHang,
            N'Tạo phiếu nhập nội bộ thành công' AS ThongBao;

    END TRY
    BEGIN CATCH
        IF @StartedTran = 1 AND @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        SET @ErrMsg = ERROR_MESSAGE();
        SET @ErrSeverity = ERROR_SEVERITY();
        SET @ErrState = ERROR_STATE();

        RAISERROR(@ErrMsg, @ErrSeverity, @ErrState);
    END CATCH;
END;
