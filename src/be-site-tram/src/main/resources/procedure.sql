USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_KiemTraTonKho_ToanHeThong]    Script Date: 02-Jun-26 3:44:45 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

/* Tra cứu tổng tồn kho một sản phẩm trên toàn hệ thống */
ALTER PROC [dbo].[sp_KiemTraTonKho_ToanHeThong]
    @MaSP VARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        MaSP,
        MAX(TenSP) AS TenSP,
        SUM(SoLuongTon) AS TongSoLuongTon,
        SUM(SoLuongDatHang) AS TongSoLuongDatHang,
        SUM(SoLuongTon - SoLuongDatHang) AS TongSoLuongKhaDung
    FROM
    (
        ------------------------------------------------------------
        -- 1. Tồn kho tại SITE_BAC - đọc trực tiếp local
        ------------------------------------------------------------
        SELECT 
            tk.MaSP,
            sp.TenSP,
            tk.SoLuongTon,
            tk.SoLuongDatHang
        FROM dbo.TonKho tk
        JOIN dbo.SanPham_Core sp 
            ON tk.MaSP = sp.MaSP
        WHERE tk.MaSP = @MaSP

        UNION ALL

        ------------------------------------------------------------
        -- 2. Tồn kho tại site còn lại - gọi qua linked server LINK
        ------------------------------------------------------------
        SELECT 
            tk.MaSP,
            sp.TenSP,
            tk.SoLuongTon,
            tk.SoLuongDatHang
        FROM [LINK].[store_management].dbo.TonKho tk
        JOIN [LINK].[store_management].dbo.SanPham_Core sp 
            ON tk.MaSP = sp.MaSP
        WHERE tk.MaSP = @MaSP
    ) AS TonKhoToanHeThong
    GROUP BY MaSP;
END;

EXEC dbo.sp_KiemTraTonKho_ToanHeThong @MaSP = 'SP001';