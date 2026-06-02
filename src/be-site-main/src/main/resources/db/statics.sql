-- Thống kê số lượng tồn, số lượng đặt hàng và số lượng khả dụng của một sản phẩm tại từng kho
DECLARE @MaSP VARCHAR(20) = 'SP001';

SELECT
    SiteNguon,
    MaKhuVuc,
    MaKho,
    TenKho,
    MaSP,
    TenSP,
    SoLuongTon,
    SoLuongDatHang,
    SoLuongTon - SoLuongDatHang AS SoLuongKhaDung
FROM
(
    SELECT
        'SITE_BAC' AS SiteNguon,
        k.MaKhuVuc,
        tk.MaKho,
        k.TenKho,
        tk.MaSP,
        sp.TenSP,
        tk.SoLuongTon,
        tk.SoLuongDatHang
    FROM [SITE_BAC].[store_management].dbo.TonKho tk
    JOIN [SITE_BAC].[store_management].dbo.Kho k
        ON tk.MaKho = k.MaKho
    JOIN [SITE_BAC].[store_management].dbo.SanPham_Core sp
        ON tk.MaSP = sp.MaSP
    WHERE tk.MaSP = @MaSP

    UNION ALL

    SELECT
        'SITE_NAM' AS SiteNguon,
        k.MaKhuVuc,
        tk.MaKho,
        k.TenKho,
        tk.MaSP,
        sp.TenSP,
        tk.SoLuongTon,
        tk.SoLuongDatHang
    FROM [SITE_NAM].[store_management].dbo.TonKho tk
    JOIN [SITE_NAM].[store_management].dbo.Kho k
        ON tk.MaKho = k.MaKho
    JOIN [SITE_NAM].[store_management].dbo.SanPham_Core sp
        ON tk.MaSP = sp.MaSP
    WHERE tk.MaSP = @MaSP
) AS T
WHERE SoLuongTon - SoLuongDatHang > 0
ORDER BY SiteNguon, MaKho;


-- Thống kê tổng số lượng tồn, số lượng đặt hàng và số lượng khả dụng của một sản phẩm trên toàn hệ thống
DECLARE @MaSP_Tong VARCHAR(20) = 'SP001';

SELECT
    MaSP,
    MAX(TenSP) AS TenSP,
    SUM(SoLuongTon) AS TongSoLuongTon,
    SUM(SoLuongDatHang) AS TongSoLuongDatHang,
    SUM(SoLuongTon - SoLuongDatHang) AS TongSoLuongKhaDung
FROM
(
    SELECT
        tk.MaSP,
        sp.TenSP,
        tk.SoLuongTon,
        tk.SoLuongDatHang
    FROM [SITE_BAC].[store_management].dbo.TonKho tk
    JOIN [SITE_BAC].[store_management].dbo.SanPham_Core sp
        ON tk.MaSP = sp.MaSP
    WHERE tk.MaSP = @MaSP_Tong

    UNION ALL

    SELECT
        tk.MaSP,
        sp.TenSP,
        tk.SoLuongTon,
        tk.SoLuongDatHang
    FROM [SITE_NAM].[store_management].dbo.TonKho tk
    JOIN [SITE_NAM].[store_management].dbo.SanPham_Core sp
        ON tk.MaSP = sp.MaSP
    WHERE tk.MaSP = @MaSP_Tong
) AS T
GROUP BY MaSP;


WITH DoanhThuTheoKho AS
(
    SELECT
        'SITE_BAC' AS SiteNguon,
        YEAR(dh.NgayDat) AS Nam,
        MONTH(dh.NgayDat) AS Thang,
        pxk.MaKhoXuat AS MaKho,
        k.TenKho,
        SUM(ctx.SoLuongXuat * ctdh.DonGia) AS DoanhThu
    FROM [SITE_BAC].[store_management].dbo.PhieuXuatKho pxk
    JOIN [SITE_BAC].[store_management].dbo.ChiTietXuatKho ctx
        ON pxk.MaPhieuXuat = ctx.MaPhieuXuat
    JOIN [SITE_BAC].[store_management].dbo.Kho k
        ON pxk.MaKhoXuat = k.MaKho
    JOIN [SITE_BAC].[store_management].dbo.DonHang dh
        ON pxk.MaDonHang = dh.MaDonHang
    JOIN [SITE_BAC].[store_management].dbo.ChiTietDonHang ctdh
        ON ctx.MaCTDH = ctdh.MaCTDH
    WHERE 
        dh.TrangThaiTT = 'paid'
        AND dh.TrangThaiDH = 'completed'
        AND pxk.TrangThaiXuat = 'exported'
    GROUP BY
        YEAR(dh.NgayDat),
        MONTH(dh.NgayDat),
        pxk.MaKhoXuat,
        k.TenKho

    UNION ALL

    SELECT
        'SITE_NAM' AS SiteNguon,
        YEAR(dh.NgayDat) AS Nam,
        MONTH(dh.NgayDat) AS Thang,
        pxk.MaKhoXuat AS MaKho,
        k.TenKho,
        SUM(ctx.SoLuongXuat * ctdh.DonGia) AS DoanhThu
    FROM [SITE_NAM].[store_management].dbo.PhieuXuatKho pxk
    JOIN [SITE_NAM].[store_management].dbo.ChiTietXuatKho ctx
        ON pxk.MaPhieuXuat = ctx.MaPhieuXuat
    JOIN [SITE_NAM].[store_management].dbo.Kho k
        ON pxk.MaKhoXuat = k.MaKho
    JOIN [SITE_NAM].[store_management].dbo.DonHang dh
        ON pxk.MaDonHang = dh.MaDonHang
    JOIN [SITE_NAM].[store_management].dbo.ChiTietDonHang ctdh
        ON ctx.MaCTDH = ctdh.MaCTDH
    WHERE 
        dh.TrangThaiTT = 'paid'
        AND dh.TrangThaiDH = 'completed'
        AND pxk.TrangThaiXuat = 'exported'
    GROUP BY
        YEAR(dh.NgayDat),
        MONTH(dh.NgayDat),
        pxk.MaKhoXuat,
        k.TenKho
)
SELECT
    SiteNguon,
    Nam,
    Thang,
    MaKho,
    TenKho,
    SUM(DoanhThu) AS DoanhThu
FROM DoanhThuTheoKho
GROUP BY
    SiteNguon,
    Nam,
    Thang,
    MaKho,
    TenKho

UNION ALL

SELECT
    'TOAN_HE_THONG' AS SiteNguon,
    Nam,
    Thang,
    NULL AS MaKho,
    N'Tất cả kho' AS TenKho,
    SUM(DoanhThu) AS DoanhThu
FROM DoanhThuTheoKho
GROUP BY
    Nam,
    Thang
ORDER BY
    Nam,
    Thang,
    SiteNguon,
    MaKho;

-- Thống kê 10 sản phẩm bán chạy nhất theo số lượng và doanh thu
SELECT TOP 10
    MaSP,
    MAX(TenSP) AS TenSP,
    SUM(SoLuongBan) AS TongSoLuongBan,
    SUM(DoanhThu) AS TongDoanhThu
FROM
(
    SELECT
        ctdh.MaSP,
        sp.TenSP,
        ctdh.SoLuong AS SoLuongBan,
        ctdh.SoLuong * ctdh.DonGia AS DoanhThu
    FROM [SITE_BAC].[store_management].dbo.DonHang dh
    JOIN [SITE_BAC].[store_management].dbo.ChiTietDonHang ctdh
        ON dh.MaDonHang = ctdh.MaDonHang
    JOIN [SITE_BAC].[store_management].dbo.SanPham_Core sp
        ON ctdh.MaSP = sp.MaSP
    WHERE 
        dh.TrangThaiTT = 'paid'
        AND dh.TrangThaiDH = 'completed'

    UNION ALL

    SELECT
        ctdh.MaSP,
        sp.TenSP,
        ctdh.SoLuong AS SoLuongBan,
        ctdh.SoLuong * ctdh.DonGia AS DoanhThu
    FROM [SITE_NAM].[store_management].dbo.DonHang dh
    JOIN [SITE_NAM].[store_management].dbo.ChiTietDonHang ctdh
        ON dh.MaDonHang = ctdh.MaDonHang
    JOIN [SITE_NAM].[store_management].dbo.SanPham_Core sp
        ON ctdh.MaSP = sp.MaSP
    WHERE 
        dh.TrangThaiTT = 'paid'
        AND dh.TrangThaiDH = 'completed'
) AS T
GROUP BY MaSP
ORDER BY
    TongSoLuongBan DESC,
    TongDoanhThu DESC;


WITH TatCaPhieuXuat AS
(
    SELECT
        'SITE_BAC' AS SiteNguon,
        pxk.MaDonHang,
        pxk.MaKhoXuat
    FROM [SITE_BAC].[store_management].dbo.PhieuXuatKho pxk
    WHERE pxk.TrangThaiXuat = 'exported'

    UNION ALL

    SELECT
        'SITE_NAM' AS SiteNguon,
        pxk.MaDonHang,
        pxk.MaKhoXuat
    FROM [SITE_NAM].[store_management].dbo.PhieuXuatKho pxk
    WHERE pxk.TrangThaiXuat = 'exported'
),
KhoXuatKhongTrung AS
(
    SELECT DISTINCT
        MaDonHang,
        MaKhoXuat
    FROM TatCaPhieuXuat
)
SELECT
    MaDonHang,
    COUNT(*) AS SoKhoXuat,
    STRING_AGG(MaKhoXuat, ', ') AS DanhSachKhoXuat
FROM KhoXuatKhongTrung
GROUP BY MaDonHang
HAVING COUNT(*) > 1
ORDER BY
    SoKhoXuat DESC,
    MaDonHang;