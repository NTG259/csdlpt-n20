USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_XacNhanXuat_GiaoKhach]    Script Date: 02/06/2026 21:20:12 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER   PROCEDURE [dbo].[sp_XacNhanXuat_GiaoKhach]
    @MaPhieuXuat UNIQUEIDENTIFIER
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE
        @MaDonHang UNIQUEIDENTIFIER,
        @MaKhoXuat VARCHAR(20),
        @MaKhoNhan VARCHAR(20),
        @TrangThaiXuat VARCHAR(30),
        @ErrMsg NVARCHAR(4000),
        @ErrSeverity INT,
        @ErrState INT;

    BEGIN TRY
        BEGIN TRANSACTION;

        SELECT
            @MaDonHang = MaDonHang,
            @MaKhoXuat = MaKhoXuat,
            @MaKhoNhan = MaKhoNhan,
            @TrangThaiXuat = TrangThaiXuat
        FROM PhieuXuatKho WITH (UPDLOCK, HOLDLOCK)
        WHERE MaPhieuXuat = @MaPhieuXuat;

        IF @MaDonHang IS NULL
        BEGIN
            RAISERROR(N'Không tìm thấy phiếu xuất kho.', 16, 1);
        END;

        IF @MaKhoNhan IS NOT NULL
        BEGIN
            RAISERROR(N'Phiếu này là phiếu xuất nội bộ, không phải phiếu xuất giao khách.', 16, 1);
        END;

        IF @TrangThaiXuat <> 'waiting_export'
        BEGIN
            RAISERROR(N'Chỉ được xác nhận phiếu xuất đang ở trạng thái waiting_export.', 16, 1);
        END;

        CREATE TABLE #TongXuat
        (
            MaSP VARCHAR(20) PRIMARY KEY,
            SoLuongXuat INT NOT NULL
        );

        INSERT INTO #TongXuat(MaSP, SoLuongXuat)
        SELECT
            MaSP,
            SUM(SoLuongXuat)
        FROM ChiTietXuatKho
        WHERE MaPhieuXuat = @MaPhieuXuat
        GROUP BY MaSP;

        IF EXISTS (
            SELECT 1
            FROM #TongXuat tx
            LEFT JOIN TonKho tk
                ON tk.MaKho = @MaKhoXuat
               AND tk.MaSP = tx.MaSP
            WHERE tk.MaSP IS NULL
               OR tk.SoLuongTon < tx.SoLuongXuat
               OR tk.SoLuongDatHang < tx.SoLuongXuat
        )
        BEGIN
            RAISERROR(N'Tồn kho hoặc số lượng đã giữ không đủ để xuất giao khách.', 16, 1);
        END;

        UPDATE tk
        SET
            tk.SoLuongTon = tk.SoLuongTon - tx.SoLuongXuat,
            tk.SoLuongDatHang = tk.SoLuongDatHang - tx.SoLuongXuat,
            tk.NgayCapNhat = SYSDATETIME()
        FROM TonKho tk
        INNER JOIN #TongXuat tx
            ON tx.MaSP = tk.MaSP
        WHERE tk.MaKho = @MaKhoXuat;

        UPDATE PhieuXuatKho
        SET TrangThaiXuat = 'exported'
        WHERE MaPhieuXuat = @MaPhieuXuat;

        UPDATE DonHang
        SET TrangThaiDH = 'shipping'
        WHERE MaDonHang = @MaDonHang
          AND TrangThaiDH = 'processing';

        COMMIT TRANSACTION;

        SELECT
            @MaPhieuXuat AS MaPhieuXuat,
            @MaDonHang AS MaDonHang,
            N'Xác nhận xuất giao khách thành công, đơn hàng chuyển sang shipping' AS ThongBao;

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
