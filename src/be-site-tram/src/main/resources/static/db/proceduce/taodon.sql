USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_DatHang_TuSiteBac_ModelB]    Script Date: 02/06/2026 20:33:06 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER   PROCEDURE [dbo].[sp_DatHang_TuSiteBac_ModelB]
    @MaND UNIQUEIDENTIFIER,
    @HoTenNguoiNhan NVARCHAR(100),
    @SoDienThoaiNhan VARCHAR(15),
    @DiaChiGiao NVARCHAR(300),
    @MaKhuVucXuLi VARCHAR(10),
    @MaKhoNhan VARCHAR(20),              -- Kho gom hàng, ví dụ KB01
    @MaKhoUuTien VARCHAR(20) = NULL,
    @PhuongThucTT VARCHAR(50) = 'COD',
    @Items dbo.OrderItemType READONLY,
    @GhiChu NVARCHAR(500) = NULL,
    @MaDonHang UNIQUEIDENTIFIER OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE
        @MaKhuVucLocal VARCHAR(10) = 'Bac',    -- Bản cài trên DB site Bắc: local = 'Bac'.
        @MaKhuVucRemote VARCHAR(10) = 'Nam';   -- Bản cài trên DB site Nam đảo lại: Local='Nam', Remote='Bac' (cùng tên SP).

    IF @PhuongThucTT <> 'COD'
    BEGIN
        RAISERROR(N'Hệ thống hiện chỉ hỗ trợ thanh toán COD.', 16, 1);
        RETURN;
    END;

    IF NOT EXISTS (SELECT 1 FROM @Items)
    BEGIN
        RAISERROR(N'Đơn hàng phải có ít nhất một sản phẩm.', 16, 1);
        RETURN;
    END;

    IF EXISTS (
        SELECT 1
        FROM @Items
        WHERE MaSP IS NULL
           OR SoLuong IS NULL
           OR SoLuong <= 0
    )
    BEGIN
        RAISERROR(N'Danh sách sản phẩm đặt hàng không hợp lệ.', 16, 1);
        RETURN;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM Kho
        WHERE MaKho = @MaKhoNhan
          AND MaKhuVuc = @MaKhuVucLocal
          AND TrangThai = 1
    )
    BEGIN
        RAISERROR(N'Kho nhận/gom hàng phải là kho đang hoạt động thuộc site Bắc.', 16, 1);
        RETURN;
    END;

    IF @MaKhoUuTien IS NOT NULL
       AND NOT EXISTS (
            SELECT 1
            FROM Kho
            WHERE MaKho = @MaKhoUuTien
              AND MaKhuVuc = @MaKhuVucLocal
              AND TrangThai = 1
       )
    BEGIN
        RAISERROR(N'Kho ưu tiên không hợp lệ hoặc không thuộc site Bắc.', 16, 1);
        RETURN;
    END;

    IF EXISTS (
        SELECT 1
        FROM @Items i
        LEFT JOIN SanPham_Core sp ON sp.MaSP = i.MaSP
        WHERE sp.MaSP IS NULL
           OR sp.TrangThai <> 1
    )
    BEGIN
        RAISERROR(N'Tồn tại sản phẩm không hợp lệ hoặc đã ngừng bán.', 16, 1);
        RETURN;
    END;

    DECLARE
        @CurrentRow INT,
        @MaxRow INT,
        @MaSP VARCHAR(20),
        @SoLuongCan INT,
        @LocalKhaDung INT,
        @LayLocal INT,
        @ConThieu INT,
        @TongTien DECIMAL(15,2),
        @KhoUuTienGiu VARCHAR(20),
        @ErrMsg NVARCHAR(4000),
        @ErrSeverity INT,
        @ErrState INT;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- =====================================================
        -- 1. Gom nhu cầu theo sản phẩm
        -- =====================================================
        CREATE TABLE #NhuCau
        (
            RowID INT IDENTITY(1,1) PRIMARY KEY,
            MaSP VARCHAR(20) NOT NULL UNIQUE,
            SoLuongCan INT NOT NULL,
            DonGia DECIMAL(15,2) NOT NULL
        );

        INSERT INTO #NhuCau(MaSP, SoLuongCan, DonGia)
        SELECT
            i.MaSP,
            SUM(i.SoLuong) AS SoLuongCan,
            sp.GiaBan
        FROM @Items i
        INNER JOIN SanPham_Core sp ON sp.MaSP = i.MaSP
        GROUP BY
            i.MaSP,
            sp.GiaBan;

        -- =====================================================
        -- 2. Bảng phân bổ giữ hàng
        -- IsRemote = 0: giữ hàng ở site Bắc
        -- IsRemote = 1: giữ hàng ở site Nam
        -- =====================================================
        CREATE TABLE #PhanBo
        (
            RowID INT IDENTITY(1,1) PRIMARY KEY,
            MaKho VARCHAR(20) NOT NULL,
            MaSP VARCHAR(20) NOT NULL,
            SoLuongGiu INT NOT NULL,
            IsRemote BIT NOT NULL
        );

        CREATE TABLE #TamGiuHang
        (
            MaKho VARCHAR(20) NOT NULL,
            MaSP VARCHAR(20) NOT NULL,
            SoLuongGiu INT NOT NULL
        );

        SET @CurrentRow = 1;
        SELECT @MaxRow = COUNT(*) FROM #NhuCau;

        -- =====================================================
        -- 3. Giữ hàng từng sản phẩm
        -- Ưu tiên giữ ở kho gom hàng KB01 trước.
        -- Nếu thiếu thì giữ ở kho Bắc khác.
        -- Nếu vẫn thiếu thì giữ ở site Nam.
        -- =====================================================
        WHILE @CurrentRow <= @MaxRow
        BEGIN
            SELECT
                @MaSP = MaSP,
                @SoLuongCan = SoLuongCan
            FROM #NhuCau
            WHERE RowID = @CurrentRow;

            SET @LocalKhaDung = 0;
            SET @KhoUuTienGiu = ISNULL(@MaKhoUuTien, @MaKhoNhan);

            SELECT
                @LocalKhaDung = ISNULL(SUM(tk.SoLuongTon - tk.SoLuongDatHang), 0)
            FROM TonKho tk WITH (UPDLOCK, HOLDLOCK)
            INNER JOIN Kho k ON k.MaKho = tk.MaKho
            WHERE tk.MaSP = @MaSP
              AND k.MaKhuVuc = @MaKhuVucLocal
              AND k.TrangThai = 1
              AND (tk.SoLuongTon - tk.SoLuongDatHang) > 0;

            SET @LayLocal = CASE
                                WHEN @LocalKhaDung >= @SoLuongCan THEN @SoLuongCan
                                ELSE @LocalKhaDung
                             END;

            SET @ConThieu = @SoLuongCan - @LayLocal;

            -- 3.1. Giữ hàng ở site Bắc nếu có
            IF @LayLocal > 0
            BEGIN
                TRUNCATE TABLE #TamGiuHang;

                INSERT INTO #TamGiuHang(MaKho, MaSP, SoLuongGiu)
                EXEC dbo.sp_GiuHang_1SanPham_NoiBo
                    @MaSP = @MaSP,
                    @SoLuongCan = @LayLocal,
                    @MaKhuVucSite = @MaKhuVucLocal,
                    @MaKhoUuTien = @KhoUuTienGiu;

                INSERT INTO #PhanBo(MaKho, MaSP, SoLuongGiu, IsRemote)
                SELECT
                    MaKho,
                    MaSP,
                    SoLuongGiu,
                    0
                FROM #TamGiuHang;
            END;

            -- 3.2. Nếu site Bắc thiếu thì giữ hàng ở site Nam
            IF @ConThieu > 0
            BEGIN
                TRUNCATE TABLE #TamGiuHang;

                INSERT INTO #TamGiuHang(MaKho, MaSP, SoLuongGiu)
                EXEC [LINK].[store_management].dbo.sp_GiuHang_1SanPham_NoiBo
                    @MaSP = @MaSP,
                    @SoLuongCan = @ConThieu,
                    @MaKhuVucSite = @MaKhuVucRemote,
                    @MaKhoUuTien = NULL;

                INSERT INTO #PhanBo(MaKho, MaSP, SoLuongGiu, IsRemote)
                SELECT
                    MaKho,
                    MaSP,
                    SoLuongGiu,
                    1
                FROM #TamGiuHang;
            END;

            SET @CurrentRow = @CurrentRow + 1;
        END;

        -- =====================================================
        -- 4. Kiểm tra tổng giữ có đủ nhu cầu không
        -- =====================================================
        IF EXISTS (
            SELECT 1
            FROM #NhuCau n
            LEFT JOIN (
                SELECT
                    MaSP,
                    SUM(SoLuongGiu) AS TongGiu
                FROM #PhanBo
                GROUP BY MaSP
            ) p ON p.MaSP = n.MaSP
            WHERE ISNULL(p.TongGiu, 0) < n.SoLuongCan
        )
        BEGIN
            RAISERROR(N'Tổng tồn kho các site vẫn không đủ hàng.', 16, 1);
        END;

        SELECT
            @TongTien = SUM(SoLuongCan * DonGia)
        FROM #NhuCau;

        SET @MaDonHang = NEWID();

        -- =====================================================
        -- 5. Tạo đơn hàng ở site Bắc
        -- Đơn đang xử lý vì hàng có thể đang chờ gom về KB01
        -- =====================================================
        INSERT INTO DonHang
        (
            MaDonHang,
            MaND,
            HoTenNguoiNhan,
            SoDienThoaiNhan,
            DiaChiGiao,
            MaKhuVucXuLi,
            TongTien,
            PhuongThucTT,
            TrangThaiTT,
            TrangThaiDH,
            GhiChu
        )
        VALUES
        (
            @MaDonHang,
            @MaND,
            @HoTenNguoiNhan,
            @SoDienThoaiNhan,
            @DiaChiGiao,
            @MaKhuVucXuLi,
            @TongTien,
            'COD',
            'waiting_cod',
            'processing',
            @GhiChu
        );

        -- =====================================================
        -- 6. Tạo chi tiết đơn hàng
        -- =====================================================
        CREATE TABLE #CTDHMap
        (
            MaCTDH UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
            MaSP VARCHAR(20) NOT NULL UNIQUE
        );

        INSERT INTO ChiTietDonHang
        (
            MaCTDH,
            MaDonHang,
            MaSP,
            SoLuong,
            DonGia
        )
        OUTPUT inserted.MaCTDH, inserted.MaSP
        INTO #CTDHMap(MaCTDH, MaSP)
        SELECT
            NEWID(),
            @MaDonHang,
            MaSP,
            SoLuongCan,
            DonGia
        FROM #NhuCau;

        -- =====================================================
        -- 7. Tạo phiếu xuất nội bộ ở site Bắc nếu hàng nằm ở kho Bắc khác KB01
        -- Ví dụ KB02 -> KB01.
        -- Không tạo phiếu nếu hàng đã nằm ở KB01.
        -- =====================================================
        DECLARE @PhanBoLocalTransfer dbo.PhanBoXuatType;

        INSERT INTO @PhanBoLocalTransfer
        (
            MaCTDH,
            MaSP,
            MaKhoXuat,
            MaKhoNhan,
            SoLuongXuat
        )
        SELECT
            c.MaCTDH,
            pb.MaSP,
            pb.MaKho AS MaKhoXuat,
            @MaKhoNhan AS MaKhoNhan,
            SUM(pb.SoLuongGiu) AS SoLuongXuat
        FROM #PhanBo pb
        INNER JOIN #CTDHMap c ON c.MaSP = pb.MaSP
        WHERE pb.IsRemote = 0
          AND pb.MaKho <> @MaKhoNhan
        GROUP BY
            c.MaCTDH,
            pb.MaSP,
            pb.MaKho;

        IF EXISTS (SELECT 1 FROM @PhanBoLocalTransfer)
        BEGIN
            -- Tạo phiếu xuất nội bộ tại site Bắc: KB02 -> KB01
            EXEC dbo.sp_TaoPhieuXuat_Batch_NoiBo
                @MaDonHang = @MaDonHang,
                @PhanBo = @PhanBoLocalTransfer,
                @TrangThaiBanDau = 'waiting_export';

            -- Tạo phiếu nhập chờ tại KB01
            EXEC dbo.sp_TaoPhieuNhap_NoiBo_Batch
                @MaDonHang = @MaDonHang,
                @PhanBo = @PhanBoLocalTransfer;
        END;

        -- =====================================================
        -- 8. Tạo phiếu xuất nội bộ ở site Nam nếu cần lấy hàng từ Nam
        -- Ví dụ KN01 -> KB01.
        -- Đồng thời tạo phiếu nhập chờ tại site Bắc.
        -- =====================================================
        DECLARE @PhanBoRemoteTransfer dbo.PhanBoXuatType;

        INSERT INTO @PhanBoRemoteTransfer
        (
            MaCTDH,
            MaSP,
            MaKhoXuat,
            MaKhoNhan,
            SoLuongXuat
        )
        SELECT
            c.MaCTDH,
            pb.MaSP,
            pb.MaKho AS MaKhoXuat,
            @MaKhoNhan AS MaKhoNhan,
            SUM(pb.SoLuongGiu) AS SoLuongXuat
        FROM #PhanBo pb
        INNER JOIN #CTDHMap c ON c.MaSP = pb.MaSP
        WHERE pb.IsRemote = 1
        GROUP BY
            c.MaCTDH,
            pb.MaSP,
            pb.MaKho;

        IF EXISTS (SELECT 1 FROM @PhanBoRemoteTransfer)
        BEGIN
            DECLARE @PhanBoRemoteJson NVARCHAR(MAX);

            SELECT @PhanBoRemoteJson =
            (
                SELECT
                    MaCTDH,
                    MaSP,
                    MaKhoXuat,
                    MaKhoNhan,
                    SoLuongXuat
                FROM @PhanBoRemoteTransfer
                FOR JSON PATH
            );

            -- Tạo phiếu xuất nội bộ tại site Nam
            EXEC [LINK].[store_management].dbo.sp_TaoPhieuXuat_Batch_NoiBo_Json
                @MaDonHang = @MaDonHang,
                @PhanBoJson = @PhanBoRemoteJson,
                @TrangThaiBanDau = 'waiting_export';

            -- Tạo phiếu nhập chờ tại site Bắc
            EXEC dbo.sp_TaoPhieuNhap_NoiBo_Batch
                @MaDonHang = @MaDonHang,
                @PhanBo = @PhanBoRemoteTransfer;
        END;

        COMMIT TRANSACTION;

        SELECT
            @MaDonHang AS MaDonHang,
            @TongTien AS TongTien,
            N'Đặt hàng thành công theo mô hình B. Hàng đã được giữ và đang chờ gom về kho xử lý.' AS ThongBao;

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
