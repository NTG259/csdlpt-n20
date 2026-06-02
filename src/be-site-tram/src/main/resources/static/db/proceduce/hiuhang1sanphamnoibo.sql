USE [store_management]
GO
/****** Object:  StoredProcedure [dbo].[sp_GiuHang_1SanPham_NoiBo]    Script Date: 02/06/2026 20:56:16 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER   PROCEDURE [dbo].[sp_GiuHang_1SanPham_NoiBo]
    @MaSP VARCHAR(20),
    @SoLuongCan INT,
    @MaKhuVucSite VARCHAR(10),
    @MaKhoUuTien VARCHAR(20) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @MaSP IS NULL
    BEGIN
        RAISERROR(N'Mã sản phẩm không được NULL.', 16, 1);
        RETURN;
    END;

    IF @SoLuongCan IS NULL OR @SoLuongCan <= 0
    BEGIN
        RAISERROR(N'Số lượng cần giữ phải lớn hơn 0.', 16, 1);
        RETURN;
    END;

    DECLARE @StartedTran BIT = 0;

    DECLARE 
        @TongKhaDung INT = 0,
        @ConLai INT,
        @MaKho VARCHAR(20),
        @KhaDung INT,
        @SoLuongLay INT,
        @ErrMsg NVARCHAR(4000),
        @ErrSeverity INT,
        @ErrState INT;

    BEGIN TRY
        IF @@TRANCOUNT = 0
        BEGIN
            SET @StartedTran = 1;
            BEGIN TRANSACTION;
        END;

        CREATE TABLE #KetQuaGiuHang
        (
            MaKho VARCHAR(20) NOT NULL,
            MaSP VARCHAR(20) NOT NULL,
            SoLuongGiu INT NOT NULL
        );

        SELECT 
            @TongKhaDung = ISNULL(SUM(tk.SoLuongTon - tk.SoLuongDatHang), 0)
        FROM TonKho tk WITH (UPDLOCK, HOLDLOCK)
        INNER JOIN Kho k ON k.MaKho = tk.MaKho
        WHERE tk.MaSP = @MaSP
          AND k.MaKhuVuc = @MaKhuVucSite
          AND k.TrangThai = 1
          AND (tk.SoLuongTon - tk.SoLuongDatHang) > 0;

        IF @TongKhaDung < @SoLuongCan
        BEGIN
            RAISERROR(N'Site hiện tại không đủ hàng để giữ.', 16, 1);
        END;

        SET @ConLai = @SoLuongCan;

        DECLARE curKho CURSOR LOCAL FAST_FORWARD FOR
            SELECT 
                tk.MaKho,
                tk.SoLuongTon - tk.SoLuongDatHang AS KhaDung
            FROM TonKho tk WITH (UPDLOCK, HOLDLOCK)
            INNER JOIN Kho k ON k.MaKho = tk.MaKho
            WHERE tk.MaSP = @MaSP
              AND k.MaKhuVuc = @MaKhuVucSite
              AND k.TrangThai = 1
              AND (tk.SoLuongTon - tk.SoLuongDatHang) > 0
            ORDER BY 
                CASE 
                    WHEN @MaKhoUuTien IS NOT NULL 
                         AND tk.MaKho = @MaKhoUuTien 
                    THEN 0 
                    ELSE 1 
                END,
                tk.SoLuongTon - tk.SoLuongDatHang DESC,
                tk.MaKho;

        OPEN curKho;

        FETCH NEXT FROM curKho INTO @MaKho, @KhaDung;

        WHILE @@FETCH_STATUS = 0 AND @ConLai > 0
        BEGIN
            SET @SoLuongLay = CASE 
                                WHEN @KhaDung >= @ConLai THEN @ConLai
                                ELSE @KhaDung
                              END;

            UPDATE TonKho
            SET 
                SoLuongDatHang = SoLuongDatHang + @SoLuongLay,
                NgayCapNhat = SYSDATETIME()
            WHERE MaKho = @MaKho
              AND MaSP = @MaSP
              AND (SoLuongTon - SoLuongDatHang) >= @SoLuongLay;

            IF @@ROWCOUNT <> 1
            BEGIN
                RAISERROR(N'Lỗi cạnh tranh tồn kho, không giữ được hàng.', 16, 1);
            END;

            INSERT INTO #KetQuaGiuHang(MaKho, MaSP, SoLuongGiu)
            VALUES (@MaKho, @MaSP, @SoLuongLay);

            SET @ConLai = @ConLai - @SoLuongLay;

            FETCH NEXT FROM curKho INTO @MaKho, @KhaDung;
        END;

        CLOSE curKho;
        DEALLOCATE curKho;

        IF @ConLai > 0
        BEGIN
            RAISERROR(N'Không giữ đủ hàng dù đã kiểm tra khả dụng.', 16, 1);
        END;

        IF @StartedTran = 1 AND @@TRANCOUNT > 0
        BEGIN
            COMMIT TRANSACTION;
        END;

        SELECT 
            MaKho, 
            MaSP, 
            SoLuongGiu
        FROM #KetQuaGiuHang;

    END TRY
    BEGIN CATCH
        IF CURSOR_STATUS('local', 'curKho') >= 0
            CLOSE curKho;

        IF CURSOR_STATUS('local', 'curKho') > -3
            DEALLOCATE curKho;

        IF @StartedTran = 1 AND @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        SET @ErrMsg = ERROR_MESSAGE();
        SET @ErrSeverity = ERROR_SEVERITY();
        SET @ErrState = ERROR_STATE();

        RAISERROR(@ErrMsg, @ErrSeverity, @ErrState);
    END CATCH;
END;
