USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_ChonKhoNhan_ToiUu]    Script Date: 02/06/2026 20:55:46 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER   PROCEDURE [dbo].[sp_ChonKhoNhan_ToiUu]
    @MaKhuVucXuLi VARCHAR(10),
    @Items dbo.OrderItemType READONLY,
    @MaKhoNhan VARCHAR(20) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (SELECT 1 FROM @Items)
    BEGIN
        RAISERROR(N'Danh sách sản phẩm đặt hàng rỗng.', 16, 1);
        RETURN;
    END;

    IF @MaKhuVucXuLi IS NULL OR LTRIM(RTRIM(@MaKhuVucXuLi)) = ''
    BEGIN
        RAISERROR(N'Mã khu vực xử lý không được rỗng.', 16, 1);
        RETURN;
    END;

    CREATE TABLE #NhuCau
    (
        MaSP VARCHAR(20) NOT NULL PRIMARY KEY,
        SoLuongCan INT NOT NULL
    );

    INSERT INTO #NhuCau(MaSP, SoLuongCan)
    SELECT 
        MaSP,
        SUM(SoLuong) AS SoLuongCan
    FROM @Items
    GROUP BY MaSP;

    CREATE TABLE #DiemKho
    (
        MaKho VARCHAR(20) NOT NULL,
        TenKho NVARCHAR(100) NULL,
        MaKhuVuc VARCHAR(10) NULL,

        TongSoMatHang INT NOT NULL,
        SoMatHangCoTon INT NOT NULL,
        SoMatHangDuTaiKho INT NOT NULL,

        TongNhuCau INT NOT NULL,
        TongSoLuongDapUng INT NOT NULL,
        TongSoLuongThieu INT NOT NULL,

        DuToanBoDonTaiKho BIT NOT NULL,
        TyLeDapUng DECIMAL(10,4) NOT NULL
    );

    INSERT INTO #DiemKho
    (
        MaKho,
        TenKho,
        MaKhuVuc,
        TongSoMatHang,
        SoMatHangCoTon,
        SoMatHangDuTaiKho,
        TongNhuCau,
        TongSoLuongDapUng,
        TongSoLuongThieu,
        DuToanBoDonTaiKho,
        TyLeDapUng
    )
    SELECT
        k.MaKho,
        k.TenKho,
        k.MaKhuVuc,

        COUNT(n.MaSP) AS TongSoMatHang,

        SUM(
            CASE 
                WHEN ISNULL(tk.SoLuongTon - tk.SoLuongDatHang, 0) > 0
                THEN 1 ELSE 0
            END
        ) AS SoMatHangCoTon,

        SUM(
            CASE 
                WHEN ISNULL(tk.SoLuongTon - tk.SoLuongDatHang, 0) >= n.SoLuongCan
                THEN 1 ELSE 0
            END
        ) AS SoMatHangDuTaiKho,

        SUM(n.SoLuongCan) AS TongNhuCau,

        SUM(
            CASE
                WHEN ISNULL(tk.SoLuongTon - tk.SoLuongDatHang, 0) >= n.SoLuongCan
                    THEN n.SoLuongCan
                WHEN ISNULL(tk.SoLuongTon - tk.SoLuongDatHang, 0) > 0
                    THEN ISNULL(tk.SoLuongTon - tk.SoLuongDatHang, 0)
                ELSE 0
            END
        ) AS TongSoLuongDapUng,

        SUM(
            CASE
                WHEN ISNULL(tk.SoLuongTon - tk.SoLuongDatHang, 0) >= n.SoLuongCan
                    THEN 0
                ELSE n.SoLuongCan - ISNULL(tk.SoLuongTon - tk.SoLuongDatHang, 0)
            END
        ) AS TongSoLuongThieu,

        CASE
            WHEN SUM(
                    CASE 
                        WHEN ISNULL(tk.SoLuongTon - tk.SoLuongDatHang, 0) >= n.SoLuongCan
                        THEN 1 ELSE 0
                    END
                 ) = COUNT(n.MaSP)
            THEN 1 ELSE 0
        END AS DuToanBoDonTaiKho,

        CAST(
            SUM(
                CASE
                    WHEN ISNULL(tk.SoLuongTon - tk.SoLuongDatHang, 0) >= n.SoLuongCan
                        THEN n.SoLuongCan
                    WHEN ISNULL(tk.SoLuongTon - tk.SoLuongDatHang, 0) > 0
                        THEN ISNULL(tk.SoLuongTon - tk.SoLuongDatHang, 0)
                    ELSE 0
                END
            ) * 1.0 / NULLIF(SUM(n.SoLuongCan), 0)
            AS DECIMAL(10,4)
        ) AS TyLeDapUng
    FROM dbo.Kho k
    CROSS JOIN #NhuCau n
    LEFT JOIN dbo.TonKho tk
        ON tk.MaKho = k.MaKho
       AND tk.MaSP = n.MaSP
    WHERE UPPER(k.MaKhuVuc) = UPPER(@MaKhuVucXuLi)
      AND k.TrangThai = 1
    GROUP BY
        k.MaKho,
        k.TenKho,
        k.MaKhuVuc;

    SELECT TOP 1
        @MaKhoNhan = MaKho
    FROM #DiemKho
    ORDER BY
        DuToanBoDonTaiKho DESC,
        SoMatHangDuTaiKho DESC,
        TongSoLuongDapUng DESC,
        TongSoLuongThieu ASC,
        MaKho ASC;

    IF @MaKhoNhan IS NULL
    BEGIN
        RAISERROR(N'Không tìm được kho nhận phù hợp trong khu vực xử lý.', 16, 1);
        RETURN;
    END;

    SELECT
        MaKho,
        TenKho,
        MaKhuVuc,
        DuToanBoDonTaiKho,
        TongSoMatHang,
        SoMatHangCoTon,
        SoMatHangDuTaiKho,
        TongNhuCau,
        TongSoLuongDapUng,
        TongSoLuongThieu,
        TyLeDapUng,
        CASE 
            WHEN MaKho = @MaKhoNhan THEN N'Được chọn'
            ELSE N'Ứng viên'
        END AS KetQuaChon
    FROM #DiemKho
    ORDER BY
        DuToanBoDonTaiKho DESC,
        SoMatHangDuTaiKho DESC,
        TongSoLuongDapUng DESC,
        TongSoLuongThieu ASC,
        MaKho ASC;
END;
