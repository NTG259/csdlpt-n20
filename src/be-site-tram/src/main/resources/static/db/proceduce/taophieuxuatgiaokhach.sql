USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_TaoPhieuXuat_GiaoKhach_KhiDuHang]    Script Date: 02/06/2026 23:19:48 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER   PROCEDURE [dbo].[sp_TaoPhieuXuat_GiaoKhach_KhiDuHang]
    @MaDonHang UNIQUEIDENTIFIER,
    @MaKhoXuat VARCHAR(20),
    @MaPhieuXuat UNIQUEIDENTIFIER OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE
        @ErrMsg NVARCHAR(4000),
        @ErrSeverity INT,
        @ErrState INT;

    BEGIN TRY
        BEGIN TRANSACTION;

        IF NOT EXISTS (
            SELECT 1
            FROM DonHang
            WHERE MaDonHang = @MaDonHang
              AND TrangThaiDH = 'processing'
        )
        BEGIN
            RAISERROR(N'Đơn hàng không tồn tại hoặc không ở trạng thái processing.', 16, 1);
        END;

        IF NOT EXISTS (
            SELECT 1
            FROM Kho
            WHERE MaKho = @MaKhoXuat
              AND TrangThai = 1
        )
        BEGIN
            RAISERROR(N'Kho xuất giao khách không hợp lệ.', 16, 1);
        END;

        -- Nếu còn phiếu nhập chờ nhận thì chưa được xuất giao khách.
        IF EXISTS (
            SELECT 1
            FROM PhieuNhapKho
            WHERE MaDonHang = @MaDonHang
              AND TrangThaiNhap <> 'imported'
        )
        BEGIN
            RAISERROR(N'Đơn hàng chưa gom đủ hàng về kho xử lý, không thể tạo phiếu xuất giao khách.', 16, 1);
        END;

        -- Kiểm tra kho xử lý có đủ hàng đã giữ không.
        IF EXISTS (
            SELECT 1
            FROM ChiTietDonHang ctdh
            LEFT JOIN TonKho tk
                ON tk.MaKho = @MaKhoXuat
               AND tk.MaSP = ctdh.MaSP
            WHERE ctdh.MaDonHang = @MaDonHang
              AND (
                    tk.MaSP IS NULL
                    OR tk.SoLuongTon < ctdh.SoLuong
                    OR tk.SoLuongDatHang < ctdh.SoLuong
              )
        )
        BEGIN
            RAISERROR(N'Kho xử lý chưa có đủ hàng đã giữ để xuất giao khách.', 16, 1);
        END;

        SELECT
            @MaPhieuXuat = MaPhieuXuat
        FROM PhieuXuatKho WITH (UPDLOCK, HOLDLOCK)
        WHERE MaDonHang = @MaDonHang
          AND MaKhoXuat = @MaKhoXuat
          AND MaKhoNhan IS NULL;

        IF @MaPhieuXuat IS NULL
        BEGIN
            SET @MaPhieuXuat = NEWID();

            INSERT INTO PhieuXuatKho
            (
                MaPhieuXuat,
                MaDonHang,
                MaKhoXuat,
                MaKhoNhan,
                TrangThaiXuat,
                TrangThaiNhan
            )
            VALUES
            (
                @MaPhieuXuat,
                @MaDonHang,
                @MaKhoXuat,
                NULL,
                'waiting_export',
                NULL
            );
        END;

        INSERT INTO ChiTietXuatKho
        (
            MaCTXK,
            MaPhieuXuat,
            MaCTDH,
            MaSP,
            SoLuongXuat
        )
        SELECT
            NEWID(),
            @MaPhieuXuat,
            ctdh.MaCTDH,
            ctdh.MaSP,
            ctdh.SoLuong
        FROM ChiTietDonHang ctdh
        WHERE ctdh.MaDonHang = @MaDonHang
          AND NOT EXISTS (
                SELECT 1
                FROM ChiTietXuatKho ctxk
                WHERE ctxk.MaPhieuXuat = @MaPhieuXuat
                  AND ctxk.MaCTDH = ctdh.MaCTDH
          );

        COMMIT TRANSACTION;

        SELECT
            @MaPhieuXuat AS MaPhieuXuat,
            N'Tạo phiếu xuất giao khách thành công' AS ThongBao;

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
