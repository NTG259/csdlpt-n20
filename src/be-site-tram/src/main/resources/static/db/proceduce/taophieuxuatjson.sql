USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_TaoPhieuXuat_Batch_NoiBo_Json]    Script Date: 02/06/2026 20:58:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER   PROCEDURE [dbo].[sp_TaoPhieuXuat_Batch_NoiBo_Json]
    @MaDonHang UNIQUEIDENTIFIER,
    @PhanBoJson NVARCHAR(MAX),
    @TrangThaiBanDau VARCHAR(30) = 'waiting_export'
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @MaDonHang IS NULL
    BEGIN
        RAISERROR(N'Mã đơn hàng không được NULL.', 16, 1);
        RETURN;
    END;

    IF @PhanBoJson IS NULL OR ISJSON(@PhanBoJson) <> 1
    BEGIN
        RAISERROR(N'Dữ liệu JSON phân bổ xuất kho không hợp lệ.', 16, 1);
        RETURN;
    END;

    IF @TrangThaiBanDau NOT IN ('waiting_export', 'exported', 'cancelled')
    BEGIN
        RAISERROR(N'Trạng thái xuất ban đầu không hợp lệ.', 16, 1);
        RETURN;
    END;

    DECLARE @PhanBo dbo.PhanBoXuatType;

    INSERT INTO @PhanBo
    (
        MaCTDH,
        MaSP,
        MaKhoXuat,
        MaKhoNhan,
        SoLuongXuat
    )
    SELECT
        MaCTDH,
        MaSP,
        MaKhoXuat,
        MaKhoNhan,
        SoLuongXuat
    FROM OPENJSON(@PhanBoJson)
    WITH
    (
        MaCTDH UNIQUEIDENTIFIER '$.MaCTDH',
        MaSP VARCHAR(20) '$.MaSP',
        MaKhoXuat VARCHAR(20) '$.MaKhoXuat',
        MaKhoNhan VARCHAR(20) '$.MaKhoNhan',
        SoLuongXuat INT '$.SoLuongXuat'
    );

    IF NOT EXISTS (SELECT 1 FROM @PhanBo)
    BEGIN
        RAISERROR(N'Dữ liệu phân bổ xuất kho remote rỗng.', 16, 1);
        RETURN;
    END;

    IF EXISTS (
        SELECT 1
        FROM @PhanBo
        WHERE MaSP IS NULL
           OR MaKhoXuat IS NULL
           OR SoLuongXuat IS NULL
           OR SoLuongXuat <= 0
    )
    BEGIN
        RAISERROR(N'Dữ liệu phân bổ xuất kho remote không hợp lệ.', 16, 1);
        RETURN;
    END;

    EXEC dbo.sp_TaoPhieuXuat_Batch_NoiBo
        @MaDonHang = @MaDonHang,
        @PhanBo = @PhanBo,
        @TrangThaiBanDau = @TrangThaiBanDau;
END;
