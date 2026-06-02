USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_XacNhanXuat_NoiBo]    Script Date: 02/06/2026 21:20:27 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER   PROCEDURE [dbo].[sp_XacNhanXuat_NoiBo]
    @MaPhieuXuat UNIQUEIDENTIFIER
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE
        @MaKhoXuat VARCHAR(20),
        @MaKhoNhan VARCHAR(20),
        @TrangThaiXuat VARCHAR(30),
        @ErrMsg NVARCHAR(4000),
        @ErrSeverity INT,
        @ErrState INT;

    BEGIN TRY
        BEGIN TRANSACTION;

        SELECT
            @MaKhoXuat = MaKhoXuat,
            @MaKhoNhan = MaKhoNhan,
            @TrangThaiXuat = TrangThaiXuat
        FROM PhieuXuatKho WITH (UPDLOCK, HOLDLOCK)
        WHERE MaPhieuXuat = @MaPhieuXuat;

        IF @MaKhoXuat IS NULL
        BEGIN
            RAISERROR(N'Không tìm thấy phiếu xuất kho.', 16, 1);
        END;

        IF @MaKhoNhan IS NULL
        BEGIN
            RAISERROR(N'Phiếu này là phiếu xuất giao khách, không phải phiếu xuất nội bộ.', 16, 1);
        END;

        IF @TrangThaiXuat <> 'waiting_export'
        BEGIN
            RAISERROR(N'Chỉ được xác nhận phiếu đang ở trạng thái waiting_export.', 16, 1);
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
            RAISERROR(N'Tồn kho hoặc số lượng đã giữ không đủ để xác nhận xuất nội bộ.', 16, 1);
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

        COMMIT TRANSACTION;

        SELECT
            @MaPhieuXuat AS MaPhieuXuat,
            N'Xác nhận xuất nội bộ thành công' AS ThongBao;

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
