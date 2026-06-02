
-- =========================================================
-- 1. KHU VUC
-- =========================================================
CREATE TABLE KhuVuc (
    MaKhuVuc VARCHAR(10) PRIMARY KEY,
    TenKhuVuc NVARCHAR(100) NOT NULL
);
GO

-- =========================================================
-- 2. KHO
-- =========================================================
CREATE TABLE Kho (
    MaKho VARCHAR(20) PRIMARY KEY,
    TenKho NVARCHAR(100) NOT NULL,
    DiaChi NVARCHAR(300) NOT NULL,
    MaKhuVuc VARCHAR(10) NOT NULL,
    SucChua INT NOT NULL DEFAULT 0,
    TrangThai TINYINT NOT NULL DEFAULT 1,

    CONSTRAINT FK_Kho_KhuVuc
        FOREIGN KEY (MaKhuVuc)
        REFERENCES KhuVuc(MaKhuVuc),

    CONSTRAINT CK_Kho_SucChua
        CHECK (SucChua >= 0),

    CONSTRAINT CK_Kho_TrangThai
        CHECK (TrangThai IN (0,1))
);
GO

-- =========================================================
-- 3. DANH MUC
-- =========================================================
CREATE TABLE DanhMuc (
    MaDanhMuc VARCHAR(20) PRIMARY KEY,
    TenDanhMuc NVARCHAR(100) NOT NULL,
    MaDanhMucCha VARCHAR(20) NULL,
    MoTa NVARCHAR(500) NULL,
    TrangThai TINYINT NOT NULL DEFAULT 1,

    CONSTRAINT FK_DanhMuc_Cha
        FOREIGN KEY (MaDanhMucCha)
        REFERENCES DanhMuc(MaDanhMuc),

    CONSTRAINT CK_DanhMuc_TrangThai
        CHECK (TrangThai IN (0,1))
);
GO

-- =========================================================
-- 4. THUONG HIEU
-- =========================================================
CREATE TABLE ThuongHieu (
    MaThuongHieu VARCHAR(20) PRIMARY KEY,
    TenThuongHieu NVARCHAR(100) NOT NULL UNIQUE,
    TrangThai TINYINT NOT NULL DEFAULT 1,

    CONSTRAINT CK_ThuongHieu_TrangThai
        CHECK (TrangThai IN (0,1))
);
GO

-- =========================================================
-- 5. DANH MUC - THUONG HIEU
-- =========================================================
CREATE TABLE DanhMuc_ThuongHieu (
    MaDanhMuc VARCHAR(20) NOT NULL,
    MaThuongHieu VARCHAR(20) NOT NULL,

    CONSTRAINT PK_DanhMuc_ThuongHieu
        PRIMARY KEY (MaDanhMuc, MaThuongHieu),

    CONSTRAINT FK_DMTH_DanhMuc
        FOREIGN KEY (MaDanhMuc)
        REFERENCES DanhMuc(MaDanhMuc),

    CONSTRAINT FK_DMTH_ThuongHieu
        FOREIGN KEY (MaThuongHieu)
        REFERENCES ThuongHieu(MaThuongHieu)
);
GO

-- =========================================================
-- 6. SAN PHAM CORE
-- =========================================================
CREATE TABLE SanPham_Core (
    MaSP VARCHAR(20) PRIMARY KEY,
    TenSP NVARCHAR(255) NOT NULL,
    MaDanhMuc VARCHAR(20) NOT NULL,
    MaThuongHieu VARCHAR(20) NOT NULL,
    GiaBan DECIMAL(15,2) NOT NULL,
    DonViTinh NVARCHAR(20) NOT NULL DEFAULT N'Cái',
    HinhAnh VARCHAR(500) NULL,
    TrangThai TINYINT NOT NULL DEFAULT 1,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_SP_DanhMuc
        FOREIGN KEY (MaDanhMuc)
        REFERENCES DanhMuc(MaDanhMuc),

    CONSTRAINT FK_SP_ThuongHieu
        FOREIGN KEY (MaThuongHieu)
        REFERENCES ThuongHieu(MaThuongHieu),

    CONSTRAINT CK_SP_GiaBan
        CHECK (GiaBan >= 0),

    CONSTRAINT CK_SP_TrangThai
        CHECK (TrangThai IN (0,1))
);
GO

-- =========================================================
-- 7. SAN PHAM DETAIL
-- =========================================================
CREATE TABLE SanPham_Detail (
    MaSP VARCHAR(20) PRIMARY KEY,
    MoTa NVARCHAR(MAX) NULL,
    ThongSoKyThuat NVARCHAR(MAX) NULL,

    CONSTRAINT FK_SPDetail_Core
        FOREIGN KEY (MaSP)
        REFERENCES SanPham_Core(MaSP),

    CONSTRAINT CK_SPDetail_JSON
        CHECK (
            ThongSoKyThuat IS NULL
            OR ISJSON(ThongSoKyThuat) = 1
        )
);
GO

-- =========================================================
-- 8. NGUOI DUNG
-- =========================================================
CREATE TABLE NguoiDung (
    MaND UNIQUEIDENTIFIER
        PRIMARY KEY DEFAULT NEWID(),

    MatKhau VARCHAR(255) NOT NULL,

    MaKV VARCHAR(10) NOT NULL,

    MaKhoPhuTrach VARCHAR(20) NULL,

    HoTen NVARCHAR(100) NOT NULL,

    Email VARCHAR(100) NOT NULL,

    SoDienThoai VARCHAR(15) NULL,

    DiaChi NVARCHAR(300) NULL,

    NgayDangKy DATE NOT NULL
        DEFAULT CONVERT(DATE, GETDATE()),

    TrangThai TINYINT NOT NULL DEFAULT 1,

    NgaySinh DATETIME2 NULL,

    GioiTinh NVARCHAR(10) NOT NULL DEFAULT N'Nam',

    CCCD VARCHAR(12) NULL,

    VaiTro VARCHAR(20) NOT NULL DEFAULT 'USER',

    CONSTRAINT FK_ND_KhuVuc
        FOREIGN KEY (MaKV)
        REFERENCES KhuVuc(MaKhuVuc),

    CONSTRAINT FK_ND_Kho
        FOREIGN KEY (MaKhoPhuTrach)
        REFERENCES Kho(MaKho),

    CONSTRAINT UQ_ND_Email
        UNIQUE (Email),

    CONSTRAINT UQ_ND_SDT
        UNIQUE (SoDienThoai),

    CONSTRAINT CK_ND_TrangThai
        CHECK (TrangThai IN (0,1)),

    CONSTRAINT CK_ND_CCCD
        CHECK (
            CCCD IS NULL
            OR LEN(CCCD) = 12
        ),

    CONSTRAINT CK_ND_VaiTro
        CHECK (
            VaiTro IN (
                'ADMIN',
                'WAREHOUSE_STAFF',
                'USER'
            )
        )
);
GO

-- =========================================================
-- 9. USER GLOBAL INDEX
-- =========================================================
CREATE TABLE User_Global_Index (
    MaND UNIQUEIDENTIFIER PRIMARY KEY,

    Email VARCHAR(100) NOT NULL UNIQUE,

    SoDienThoai VARCHAR(15) NULL UNIQUE,

    MaKhuVuc VARCHAR(10) NOT NULL,

    NgayTao DATETIME2 NOT NULL
        DEFAULT SYSDATETIME(),

    CONSTRAINT FK_UGI_KhuVuc
        FOREIGN KEY (MaKhuVuc)
        REFERENCES KhuVuc(MaKhuVuc)
);
GO

-- =========================================================
-- 10. GIO HANG
-- =========================================================
CREATE TABLE GioHang (
    MaGioHang UNIQUEIDENTIFIER
        PRIMARY KEY DEFAULT NEWID(),

    MaND UNIQUEIDENTIFIER NOT NULL,

    NgayTao DATETIME2 NOT NULL
        DEFAULT SYSDATETIME(),

    NgayCapNhat DATETIME2 NOT NULL
        DEFAULT SYSDATETIME(),

    TrangThai VARCHAR(30) NOT NULL
        DEFAULT 'active',

    CONSTRAINT FK_GH_NguoiDung
        FOREIGN KEY (MaND)
        REFERENCES NguoiDung(MaND),

    CONSTRAINT CK_GH_TrangThai
        CHECK (
            TrangThai IN (
                'active',
                'ordered',
                'cancelled',
                'expired'
            )
        )
);
GO

CREATE UNIQUE INDEX UX_GioHang_Active
ON GioHang(MaND)
WHERE TrangThai = 'active';
GO

-- =========================================================
-- 11. CHI TIET GIO HANG
-- =========================================================
CREATE TABLE ChiTietGioHang (
    MaCTGH UNIQUEIDENTIFIER
        PRIMARY KEY DEFAULT NEWID(),

    MaGioHang UNIQUEIDENTIFIER NOT NULL,

    MaSP VARCHAR(20) NOT NULL,

    SoLuong INT NOT NULL,

    NgayThem DATETIME2 NOT NULL
        DEFAULT SYSDATETIME(),

    CONSTRAINT FK_CTGH_GioHang
        FOREIGN KEY (MaGioHang)
        REFERENCES GioHang(MaGioHang),

    CONSTRAINT FK_CTGH_SanPham
        FOREIGN KEY (MaSP)
        REFERENCES SanPham_Core(MaSP),

    CONSTRAINT CK_CTGH_SoLuong
        CHECK (SoLuong > 0),

    CONSTRAINT UQ_CTGH_GioHang_SP
        UNIQUE (MaGioHang, MaSP)
);
GO

-- =========================================================
-- 12. DON HANG
-- =========================================================
CREATE TABLE DonHang (
    MaDonHang UNIQUEIDENTIFIER
        PRIMARY KEY DEFAULT NEWID(),

    MaND UNIQUEIDENTIFIER NOT NULL,

    NgayDat DATETIME2 NOT NULL
        DEFAULT SYSDATETIME(),

    HoTenNguoiNhan NVARCHAR(100) NOT NULL,

    SoDienThoaiNhan VARCHAR(15) NOT NULL,

    DiaChiGiao NVARCHAR(300) NOT NULL,

    MaKhuVucXuLi VARCHAR(10) NOT NULL,   -- tên cột thực tế

    TongTien DECIMAL(15,2) NOT NULL
        DEFAULT 0,

    PhuongThucTT VARCHAR(20) NOT NULL
        DEFAULT 'COD',

    TrangThaiTT VARCHAR(30) NOT NULL
        DEFAULT 'waiting_cod',

    TrangThaiDH VARCHAR(30) NOT NULL
        DEFAULT 'pending',

    GhiChu NVARCHAR(500) NULL,

    CONSTRAINT FK_DH_NguoiDung
        FOREIGN KEY (MaND)
        REFERENCES NguoiDung(MaND),

    CONSTRAINT FK_DH_KhuVuc
        FOREIGN KEY (MaKhuVucXuLi)   -- ← sửa từ MaKhuVucGiao → MaKhuVucXuLi
        REFERENCES KhuVuc(MaKhuVuc),

    CONSTRAINT CK_DH_TongTien
        CHECK (TongTien >= 0),

    CONSTRAINT CK_DH_PhuongThucTT
        CHECK (PhuongThucTT = 'COD'),

    CONSTRAINT CK_DH_TrangThaiTT
        CHECK (
            TrangThaiTT IN (
                'waiting_cod',
                'paid',
                'failed',
                'cancelled'
            )
        ),

    CONSTRAINT CK_DH_TrangThaiDH
        CHECK (
            TrangThaiDH IN (
                'pending',
                'processing',
                'shipping',
                'completed',
                'cancelled'
            )
        )
);
GO


-- =========================================================
-- 13. CHI TIET DON HANG
-- =========================================================
CREATE TABLE ChiTietDonHang (
    MaCTDH UNIQUEIDENTIFIER
        PRIMARY KEY DEFAULT NEWID(),

    MaDonHang UNIQUEIDENTIFIER NOT NULL,

    MaSP VARCHAR(20) NOT NULL,

    SoLuong INT NOT NULL,

    DonGia DECIMAL(15,2) NOT NULL,

    ThanhTien AS (SoLuong * DonGia) PERSISTED,

    -- GIỮ FK vì ChiTietDonHang phân mảnh dẫn xuất theo DonHang
    CONSTRAINT FK_CTDH_DonHang
        FOREIGN KEY (MaDonHang)
        REFERENCES DonHang(MaDonHang),

    -- GIỮ FK vì SanPham_Core nhân bản toàn phần
    CONSTRAINT FK_CTDH_SanPham
        FOREIGN KEY (MaSP)
        REFERENCES SanPham_Core(MaSP),

    CONSTRAINT CK_CTDH_SoLuong
        CHECK (SoLuong > 0),

    CONSTRAINT CK_CTDH_DonGia
        CHECK (DonGia >= 0)
);
GO


-- =========================================================
-- 14. PHIEU XUAT KHO
-- =========================================================
CREATE TABLE PhieuXuatKho (
    MaPhieuXuat UNIQUEIDENTIFIER
        PRIMARY KEY DEFAULT NEWID(),

    -- Khóa logic tới DonHang.
    -- Không tạo FK vì DonHang có thể nằm ở site khác.
    MaDonHang UNIQUEIDENTIFIER NOT NULL,

    -- Kho xuất hàng.
    -- Giữ FK vì PhieuXuatKho phân mảnh theo MaKhoXuat.
 MaKhoXuat VARCHAR(20) NOT NULL, 

    -- Kho nhận hàng.
    -- NULL nếu xuất trực tiếp cho khách.
    -- Không tạo FK vì kho nhận có thể thuộc site khác.
 MaKhoNhan VARCHAR(20) NULL, 

    NgayTao DATETIME2 NOT NULL
        DEFAULT SYSDATETIME(),

    TrangThaiXuat VARCHAR(30) NOT NULL
        DEFAULT 'waiting_export',

    TrangThaiNhan VARCHAR(30) NULL,

    -- GIỮ FK với kho xuất vì cùng site
    CONSTRAINT FK_PXK_Kho_Xuat
        FOREIGN KEY (MaKhoXuat)
        REFERENCES Kho(MaKho),

    CONSTRAINT CK_PXK_TrangThaiXuat
        CHECK (
            TrangThaiXuat IN (
                'waiting_export',
                'exported',
                'cancelled'
            )
        ),

    CONSTRAINT CK_PXK_TrangThaiNhan
        CHECK (
            TrangThaiNhan IS NULL
            OR TrangThaiNhan IN (
                'waiting_receive',
                'received'
            )
        ),

    CONSTRAINT CK_PXK_TrangThaiNhan_Theo_KhoNhan
        CHECK (
            (MaKhoNhan IS NULL AND TrangThaiNhan IS NULL)
            OR
            (MaKhoNhan IS NOT NULL AND TrangThaiNhan IS NOT NULL)
        ),

    CONSTRAINT CK_PXK_KhoNhan_Khac_KhoXuat
        CHECK (
            MaKhoNhan IS NULL
            OR MaKhoNhan <> MaKhoXuat
        )
);
GO


-- =========================================================
-- 15. CHI TIET XUAT KHO
-- =========================================================
CREATE TABLE ChiTietXuatKho (
    MaCTXK UNIQUEIDENTIFIER
        PRIMARY KEY DEFAULT NEWID(),

    MaPhieuXuat UNIQUEIDENTIFIER NOT NULL,

    -- Khóa logic tới ChiTietDonHang.
    -- Không tạo FK vì ChiTietDonHang có thể nằm ở site khác.
    MaCTDH UNIQUEIDENTIFIER NULL,

 MaSP VARCHAR(20) NOT NULL,

    SoLuongXuat INT NOT NULL,

    -- GIỮ FK vì ChiTietXuatKho phân mảnh dẫn xuất theo MaPhieuXuat
    CONSTRAINT FK_CTXK_PhieuXuat
        FOREIGN KEY (MaPhieuXuat)
        REFERENCES PhieuXuatKho(MaPhieuXuat),

    -- GIỮ FK vì SanPham_Core nhân bản toàn phần
    CONSTRAINT FK_CTXK_SanPham
        FOREIGN KEY (MaSP)
        REFERENCES SanPham_Core(MaSP),

    CONSTRAINT CK_CTXK_SoLuongXuat
        CHECK (SoLuongXuat > 0)
);
GO


-- =========================================================
-- 16. TON KHO
-- =========================================================
CREATE TABLE TonKho (
    MaTonKho UNIQUEIDENTIFIER
        PRIMARY KEY DEFAULT NEWID(),

  MaKho VARCHAR(20) NOT NULL, 

  MaSP VARCHAR(20) NOT NULL, 

    SoLuongTon INT NOT NULL,

    SoLuongDatHang INT NOT NULL
        DEFAULT 0,

    NgayCapNhat DATETIME2 NOT NULL
        DEFAULT SYSDATETIME(),

    RowVer ROWVERSION,

    -- GIỮ FK vì TonKho và Kho cùng site
    CONSTRAINT FK_TK_Kho
        FOREIGN KEY (MaKho)
        REFERENCES Kho(MaKho),

    -- GIỮ FK vì SanPham_Core nhân bản toàn phần
    CONSTRAINT FK_TK_SanPham
        FOREIGN KEY (MaSP)
        REFERENCES SanPham_Core(MaSP),

    CONSTRAINT UQ_TonKho_MaKho_MaSP
        UNIQUE (MaKho, MaSP),

    CONSTRAINT CK_TK_SoLuongTon
        CHECK (SoLuongTon >= 0),

    CONSTRAINT CK_TK_SoLuongDatHang
        CHECK (SoLuongDatHang >= 0),

    CONSTRAINT CK_TK_KhaDung
        CHECK (SoLuongTon >= SoLuongDatHang)
);
GO


-- =========================================================
-- 17. PHIEU NHAP KHO
-- =========================================================
CREATE TABLE PhieuNhapKho (
    MaPhieuNhap UNIQUEIDENTIFIER
        PRIMARY KEY DEFAULT NEWID(),

    MaKhoXuat VARCHAR(20) NOT NULL,
    MaKhoNhap VARCHAR(20) NOT NULL,
    NgayNhap DATETIME2 NOT NULL
        DEFAULT SYSDATETIME(),

    MaNhanVienNhap UNIQUEIDENTIFIER NOT NULL,

    TrangThaiNhap VARCHAR(30) NOT NULL
        DEFAULT 'waiting_import',

    GhiChu NVARCHAR(500) NULL,

    CONSTRAINT FK_PNK_Kho_Xuat
        FOREIGN KEY (MaKhoXuat)
        REFERENCES Kho(MaKho),
    CONSTRAINT FK_PNK_Kho_Nhap
        FOREIGN KEY (MaKhoNhap)
        REFERENCES Kho(MaKho),

    CONSTRAINT FK_PNK_NguoiDung
        FOREIGN KEY (MaNhanVienNhap)
        REFERENCES NguoiDung(MaND)
);
GO

-- =========================================================
-- 18. CHI TIET PHIEU NHAP
-- =========================================================
CREATE TABLE ChiTietPhieuNhap (
    MaCTPN UNIQUEIDENTIFIER
        PRIMARY KEY DEFAULT NEWID(),

    MaPhieuNhap UNIQUEIDENTIFIER NOT NULL,

    MaSP VARCHAR(20) NOT NULL,

    SoLuong INT NOT NULL,

    DonGiaNhap DECIMAL(15,2) NOT NULL,

    CONSTRAINT FK_CTPN_PhieuNhap
        FOREIGN KEY (MaPhieuNhap)
        REFERENCES PhieuNhapKho(MaPhieuNhap),

    CONSTRAINT FK_CTPN_SanPham
        FOREIGN KEY (MaSP)
        REFERENCES SanPham_Core(MaSP),

    CONSTRAINT CK_CTPN_SoLuong
        CHECK (SoLuong > 0),

    CONSTRAINT CK_CTPN_DonGia
        CHECK (DonGiaNhap >= 0)
);
GO


