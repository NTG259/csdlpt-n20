USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_ThongKeDoanhThu_ToanHeThong]    Script Date: 02/06/2026 17:34:40 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER   PROCEDURE [dbo].[sp_ThongKeDoanhThu_ToanHeThong]
    @TuNgay DATETIME2 = NULL,
    @DenNgay DATETIME2 = NULL,
    @MaKho VARCHAR(20) = NULL,
    @MaKhuVuc VARCHAR(10) = NULL,
    @MaSP VARCHAR(20) = NULL,
    @ChiTinhDaXuat BIT = 1
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    /*
        @ChiTinhDaXuat = 1:
            Chỉ tính doanh thu từ các phiếu xuất đã xuất kho thành công.
            Tức là PhieuXuatKho.TrangThaiXuat = 'exported'.

        @ChiTinhDaXuat = 0:
            Tính cả các phiếu xuất chưa exported.
            Dùng khi muốn xem doanh thu dự kiến.
    */

    CREATE TABLE #DongDonHangAll
    (
        SiteDonHang VARCHAR(10) NOT NULL,
        MaDonHang UNIQUEIDENTIFIER NOT NULL,
        MaCTDH UNIQUEIDENTIFIER NOT NULL,
        MaSP VARCHAR(20) NOT NULL,
        DonGia DECIMAL(15,2) NOT NULL,
        TrangThaiDH VARCHAR(30) NULL,
        TrangThaiTT VARCHAR(30) NULL
    );

    CREATE TABLE #XuatKhoAll
    (
        SiteXuat VARCHAR(10) NOT NULL,
        MaPhieuXuat UNIQUEIDENTIFIER NOT NULL,
        MaDonHang UNIQUEIDENTIFIER NOT NULL,
        MaKhoXuat VARCHAR(20) NOT NULL,
        TenKho NVARCHAR(100) NULL,
        MaKhuVuc VARCHAR(10) NULL,
        MaCTDH UNIQUEIDENTIFIER NOT NULL,
        MaSP VARCHAR(20) NOT NULL,
        TenSP NVARCHAR(255) NULL,
        SoLuongXuat INT NOT NULL,
        TrangThaiXuat VARCHAR(30) NULL,
        TrangThaiGiao VARCHAR(30) NULL,
        NgayTao DATETIME2 NULL
    );

    ------------------------------------------------------------
    -- 1. Gom chi tiết đơn hàng từ site Bắc
    ------------------------------------------------------------
    INSERT INTO #DongDonHangAll
    (
        SiteDonHang,
        MaDonHang,
        MaCTDH,
        MaSP,
        DonGia,
        TrangThaiDH,
        TrangThaiTT
    )
    SELECT
        'BAC' AS SiteDonHang,
        dh.MaDonHang,
        ctdh.MaCTDH,
        ctdh.MaSP,
        ctdh.DonGia,
        dh.TrangThaiDH,
        dh.TrangThaiTT
    FROM [SITE_BAC].[store_management].dbo.DonHang dh
    INNER JOIN [SITE_BAC].[store_management].dbo.ChiTietDonHang ctdh
        ON ctdh.MaDonHang = dh.MaDonHang;

    ------------------------------------------------------------
    -- 2. Gom chi tiết đơn hàng từ site store_management
    ------------------------------------------------------------
    INSERT INTO #DongDonHangAll
    (
        SiteDonHang,
        MaDonHang,
        MaCTDH,
        MaSP,
        DonGia,
        TrangThaiDH,
        TrangThaiTT
    )
    SELECT
        'NAM' AS SiteDonHang,
        dh.MaDonHang,
        ctdh.MaCTDH,
        ctdh.MaSP,
        ctdh.DonGia,
        dh.TrangThaiDH,
        dh.TrangThaiTT
    FROM [SITE_NAM].[store_management].dbo.DonHang dh
    INNER JOIN [SITE_NAM].[store_management].dbo.ChiTietDonHang ctdh
        ON ctdh.MaDonHang = dh.MaDonHang;

    ------------------------------------------------------------
    -- 3. Gom phiếu xuất kho từ site Bắc
    ------------------------------------------------------------
    INSERT INTO #XuatKhoAll
    (
        SiteXuat,
        MaPhieuXuat,
        MaDonHang,
        MaKhoXuat,
        TenKho,
        MaKhuVuc,
        MaCTDH,
        MaSP,
        TenSP,
        SoLuongXuat,
        TrangThaiXuat,
        TrangThaiGiao,
        NgayTao
    )
    SELECT
        'BAC' AS SiteXuat,
        px.MaPhieuXuat,
        px.MaDonHang,
        px.MaKhoXuat,
        k.TenKho,
        k.MaKhuVuc,
        ctxk.MaCTDH,
        ctxk.MaSP,
        sp.TenSP,
        ctxk.SoLuongXuat,
        px.TrangThaiXuat,
        px.TrangThaiNhan,
        px.NgayTao
    FROM [SITE_BAC].[store_management].dbo.PhieuXuatKho px
    INNER JOIN [SITE_BAC].[store_management].dbo.ChiTietXuatKho ctxk
        ON ctxk.MaPhieuXuat = px.MaPhieuXuat
    INNER JOIN [SITE_BAC].[store_management].dbo.Kho k
        ON k.MaKho = px.MaKhoXuat
    LEFT JOIN [SITE_BAC].[store_management].dbo.SanPham_Core sp
        ON sp.MaSP = ctxk.MaSP
    WHERE
        (@TuNgay IS NULL OR px.NgayTao >= @TuNgay)
        AND (@DenNgay IS NULL OR px.NgayTao < DATEADD(DAY, 1, @DenNgay))
        AND (@MaKho IS NULL OR px.MaKhoXuat = @MaKho)
        AND (@MaKhuVuc IS NULL OR k.MaKhuVuc = @MaKhuVuc)
        AND (@MaSP IS NULL OR ctxk.MaSP = @MaSP)
        AND (
            @ChiTinhDaXuat = 0
            OR px.TrangThaiXuat = 'exported'
        );

    ------------------------------------------------------------
    -- 4. Gom phiếu xuất kho từ site store_management
    ------------------------------------------------------------
    INSERT INTO #XuatKhoAll
    (
        SiteXuat,
        MaPhieuXuat,
        MaDonHang,
        MaKhoXuat,
        TenKho,
        MaKhuVuc,
        MaCTDH,
        MaSP,
        TenSP,
        SoLuongXuat,
        TrangThaiXuat,
        TrangThaiGiao,
        NgayTao
    )
    SELECT
        'NAM' AS SiteXuat,
        px.MaPhieuXuat,
        px.MaDonHang,
        px.MaKhoXuat,
        k.TenKho,
        k.MaKhuVuc,
        ctxk.MaCTDH,
        ctxk.MaSP,
        sp.TenSP,
        ctxk.SoLuongXuat,
        px.TrangThaiXuat,
        px.TrangThaiNhan,
        px.NgayTao
    FROM [SITE_NAM].[store_management].dbo.PhieuXuatKho px
    INNER JOIN [SITE_NAM].[store_management].dbo.ChiTietXuatKho ctxk
        ON ctxk.MaPhieuXuat = px.MaPhieuXuat
    INNER JOIN [SITE_NAM].[store_management].dbo.Kho k
        ON k.MaKho = px.MaKhoXuat
    LEFT JOIN [SITE_NAM].[store_management].dbo.SanPham_Core sp
        ON sp.MaSP = ctxk.MaSP
    WHERE
        (@TuNgay IS NULL OR px.NgayTao >= @TuNgay)
        AND (@DenNgay IS NULL OR px.NgayTao < DATEADD(DAY, 1, @DenNgay))
        AND (@MaKho IS NULL OR px.MaKhoXuat = @MaKho)
        AND (@MaKhuVuc IS NULL OR k.MaKhuVuc = @MaKhuVuc)
        AND (@MaSP IS NULL OR ctxk.MaSP = @MaSP)
        AND (
            @ChiTinhDaXuat = 0
            OR px.TrangThaiXuat = 'exported'
        );

    ------------------------------------------------------------
    -- 5. Bảng dữ liệu doanh thu chuẩn
    ------------------------------------------------------------
    CREATE TABLE #DoanhThuAll
    (
        SiteDonHang VARCHAR(10) NOT NULL,
        SiteXuat VARCHAR(10) NOT NULL,
        MaDonHang UNIQUEIDENTIFIER NOT NULL,
        MaPhieuXuat UNIQUEIDENTIFIER NOT NULL,
        MaKhoXuat VARCHAR(20) NOT NULL,
        TenKho NVARCHAR(100) NULL,
        MaKhuVuc VARCHAR(10) NULL,
        MaSP VARCHAR(20) NOT NULL,
        TenSP NVARCHAR(255) NULL,
        SoLuongXuat INT NOT NULL,
        DonGia DECIMAL(15,2) NOT NULL,
        ThanhTien DECIMAL(18,2) NOT NULL,
        TrangThaiXuat VARCHAR(30) NULL,
        TrangThaiGiao VARCHAR(30) NULL,
        NgayTao DATETIME2 NULL
    );

    INSERT INTO #DoanhThuAll
    (
        SiteDonHang,
        SiteXuat,
        MaDonHang,
        MaPhieuXuat,
        MaKhoXuat,
        TenKho,
        MaKhuVuc,
        MaSP,
        TenSP,
        SoLuongXuat,
        DonGia,
        ThanhTien,
        TrangThaiXuat,
        TrangThaiGiao,
        NgayTao
    )
    SELECT
        ddh.SiteDonHang,
        xk.SiteXuat,
        xk.MaDonHang,
        xk.MaPhieuXuat,
        xk.MaKhoXuat,
        xk.TenKho,
        xk.MaKhuVuc,
        xk.MaSP,
        xk.TenSP,
        xk.SoLuongXuat,
        ddh.DonGia,
        CAST(xk.SoLuongXuat * ddh.DonGia AS DECIMAL(18,2)) AS ThanhTien,
        xk.TrangThaiXuat,
        xk.TrangThaiGiao,
        xk.NgayTao
    FROM #XuatKhoAll xk
    INNER JOIN #DongDonHangAll ddh
        ON ddh.MaCTDH = xk.MaCTDH
       AND ddh.MaDonHang = xk.MaDonHang;

    ------------------------------------------------------------
    -- Kết quả 1: Doanh thu theo từng kho
    ------------------------------------------------------------
    SELECT
        SiteXuat,
        MaKhuVuc,
        MaKhoXuat,
        TenKho,
        COUNT(DISTINCT MaDonHang) AS SoDonHang,
        COUNT(DISTINCT MaPhieuXuat) AS SoPhieuXuat,
        SUM(SoLuongXuat) AS TongSoLuongXuat,
        SUM(ThanhTien) AS DoanhThu
    FROM #DoanhThuAll
    GROUP BY
        SiteXuat,
        MaKhuVuc,
        MaKhoXuat,
        TenKho
    ORDER BY
        SiteXuat,
        MaKhuVuc,
        MaKhoXuat;

    ------------------------------------------------------------
    -- Kết quả 2: Doanh thu theo vùng
    ------------------------------------------------------------
    SELECT
        MaKhuVuc,
        COUNT(DISTINCT MaDonHang) AS SoDonHang,
        COUNT(DISTINCT MaPhieuXuat) AS SoPhieuXuat,
        COUNT(DISTINCT MaKhoXuat) AS SoKhoThamGiaXuat,
        SUM(SoLuongXuat) AS TongSoLuongXuat,
        SUM(ThanhTien) AS DoanhThu
    FROM #DoanhThuAll
    GROUP BY MaKhuVuc
    ORDER BY MaKhuVuc;

    ------------------------------------------------------------
    -- Kết quả 3: Doanh thu toàn hệ thống
    ------------------------------------------------------------
    SELECT
        COUNT(DISTINCT MaDonHang) AS TongSoDonHang,
        COUNT(DISTINCT MaPhieuXuat) AS TongSoPhieuXuat,
        COUNT(DISTINCT MaKhoXuat) AS TongSoKhoThamGiaXuat,
        SUM(SoLuongXuat) AS TongSoLuongXuat,
        SUM(ThanhTien) AS TongDoanhThu
    FROM #DoanhThuAll;
END;
