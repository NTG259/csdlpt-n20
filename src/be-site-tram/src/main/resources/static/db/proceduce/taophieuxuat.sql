USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_TaoPhieuXuat_Batch_NoiBo]    Script Date: 02/06/2026 20:57:50 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER   PROCEDURE [dbo].[sp_TaoPhieuXuat_Batch_NoiBo]
    @MaDonHang UNIQUEIDENTIFIER,
    @PhanBo dbo.PhanBoXuatType READONLY,
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

    IF NOT EXISTS (SELECT 1 FROM @PhanBo)
    BEGIN
        RAISERROR(N'Danh sách phân bổ xuất kho rỗng.', 16, 1);
        RETURN;
    END;

    IF @TrangThaiBanDau NOT IN ('waiting_export', 'exported', 'cancelled')
    BEGIN
        RAISERROR(N'Trạng thái xuất ban đầu không hợp lệ.', 16, 1);
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
        RAISERROR(N'Dữ liệu phân bổ xuất kho không hợp lệ.', 16, 1);
        RETURN;
    END;

    DECLARE @StartedTran BIT = 0;

    DECLARE 
        @ErrMsg NVARCHAR(4000),
        @ErrSeverity INT,
        @ErrState INT;

    BEGIN TRY
        IF @@TRANCOUNT = 0
        BEGIN
            SET @StartedTran = 1;
            BEGIN TRANSACTION;
        END;

        -- =====================================================
        -- 1. Chuẩn hóa phân bổ
        -- Nếu MaKhoXuat = MaKhoNhan thì MaKhoNhan phải NULL
        -- =====================================================
        CREATE TABLE #PhanBoNorm
        (
            MaCTDH UNIQUEIDENTIFIER NULL,
            MaSP VARCHAR(20) NOT NULL,
            MaKhoXuat VARCHAR(20) NOT NULL,
            MaKhoNhan VARCHAR(20) NULL,
            SoLuongXuat INT NOT NULL
        );

        INSERT INTO #PhanBoNorm
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
            CASE 
                WHEN MaKhoNhan = MaKhoXuat THEN NULL
                ELSE MaKhoNhan
            END AS MaKhoNhan,
            SoLuongXuat
        FROM @PhanBo;

        -- =====================================================
        -- 2. Tạo map phiếu xuất
        -- Mỗi cặp MaKhoXuat + MaKhoNhan tạo 1 phiếu
        -- MaKhoNhan NULL nghĩa là xuất trực tiếp cho khách
        -- =====================================================
        CREATE TABLE #PXKMap
        (
            MaKhoXuat VARCHAR(20) NOT NULL,
            MaKhoNhan VARCHAR(20) NULL,
            MaKhoNhanKey VARCHAR(30) NOT NULL,
            MaPhieuXuat UNIQUEIDENTIFIER NOT NULL,

            CONSTRAINT PK_PXKMap
                PRIMARY KEY (MaKhoXuat, MaKhoNhanKey)
        );

        INSERT INTO #PXKMap
        (
            MaKhoXuat,
            MaKhoNhan,
            MaKhoNhanKey,
            MaPhieuXuat
        )
        SELECT
            x.MaKhoXuat,
            x.MaKhoNhan,
            ISNULL(x.MaKhoNhan, '__NULL__') AS MaKhoNhanKey,
            ISNULL(px.MaPhieuXuat, NEWID()) AS MaPhieuXuat
        FROM (
            SELECT DISTINCT 
                MaKhoXuat,
                MaKhoNhan
            FROM #PhanBoNorm
        ) x
        LEFT JOIN PhieuXuatKho px WITH (UPDLOCK, HOLDLOCK)
            ON px.MaDonHang = @MaDonHang
           AND px.MaKhoXuat = x.MaKhoXuat
           AND (
                (px.MaKhoNhan IS NULL AND x.MaKhoNhan IS NULL)
                OR
                (px.MaKhoNhan = x.MaKhoNhan)
           );

        -- =====================================================
        -- 3. Tạo phiếu xuất kho nếu chưa có
        -- =====================================================
        INSERT INTO PhieuXuatKho
        (
            MaPhieuXuat,
            MaDonHang,
            MaKhoXuat,
            MaKhoNhan,
            TrangThaiXuat,
            TrangThaiNhan
        )
        SELECT
            m.MaPhieuXuat,
            @MaDonHang,
            m.MaKhoXuat,
            m.MaKhoNhan,
            @TrangThaiBanDau,

            CASE
                WHEN m.MaKhoNhan IS NULL THEN NULL
                ELSE 'waiting_receive'
            END AS TrangThaiNhan
        FROM #PXKMap m
        WHERE NOT EXISTS (
            SELECT 1
            FROM PhieuXuatKho px
            WHERE px.MaDonHang = @MaDonHang
              AND px.MaKhoXuat = m.MaKhoXuat
              AND (
                    (px.MaKhoNhan IS NULL AND m.MaKhoNhan IS NULL)
                    OR
                    (px.MaKhoNhan = m.MaKhoNhan)
              )
        );

        -- =====================================================
        -- 4. Tạo chi tiết xuất kho
        -- Không còn TrangThaiXuat ở ChiTietXuatKho
        -- =====================================================
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
            m.MaPhieuXuat,
            p.MaCTDH,
            p.MaSP,
            SUM(p.SoLuongXuat) AS SoLuongXuat
        FROM #PhanBoNorm p
        INNER JOIN #PXKMap m
            ON m.MaKhoXuat = p.MaKhoXuat
           AND m.MaKhoNhanKey = ISNULL(p.MaKhoNhan, '__NULL__')
        GROUP BY
            m.MaPhieuXuat,
            p.MaCTDH,
            p.MaSP;

        IF @StartedTran = 1 AND @@TRANCOUNT > 0
        BEGIN
            COMMIT TRANSACTION;
        END;

        SELECT 
            @MaDonHang AS MaDonHang,
            N'Tạo phiếu xuất theo lô thành công' AS ThongBao;

    END TRY
    BEGIN CATCH
        IF @StartedTran = 1 AND @@TRANCOUNT > 0
        BEGIN
            ROLLBACK TRANSACTION;
        END;

        SET @ErrMsg = ERROR_MESSAGE();
        SET @ErrSeverity = ERROR_SEVERITY();
        SET @ErrState = ERROR_STATE();

        RAISERROR(@ErrMsg, @ErrSeverity, @ErrState);
    END CATCH;
END;
