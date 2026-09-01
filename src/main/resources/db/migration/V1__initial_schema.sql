CREATE TABLE user_accounts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY, first_name NVARCHAR(80) NOT NULL, last_name NVARCHAR(80) NOT NULL,
    email NVARCHAR(180) NOT NULL UNIQUE, password NVARCHAR(255) NOT NULL, role NVARCHAR(40) NOT NULL,
    active BIT NOT NULL DEFAULT 1, membership_number NVARCHAR(30) NULL UNIQUE, phone NVARCHAR(30) NULL,
    created_at DATETIME2 NOT NULL
);
CREATE TABLE books (
    id BIGINT IDENTITY(1,1) PRIMARY KEY, isbn NVARCHAR(20) NOT NULL UNIQUE, title NVARCHAR(220) NOT NULL,
    author NVARCHAR(160) NOT NULL, category NVARCHAR(100) NOT NULL, publisher NVARCHAR(160) NULL,
    publication_year INT NULL, description NVARCHAR(2000) NULL, cover_url NVARCHAR(500) NULL,
    ebook_url NVARCHAR(500) NULL, active BIT NOT NULL DEFAULT 1, created_at DATETIME2 NOT NULL
);
CREATE TABLE book_copies (
    id BIGINT IDENTITY(1,1) PRIMARY KEY, book_id BIGINT NOT NULL REFERENCES books(id),
    barcode NVARCHAR(50) NOT NULL UNIQUE, shelf_location NVARCHAR(60) NOT NULL,
    status NVARCHAR(20) NOT NULL, acquired_date DATE NULL
);
CREATE TABLE loans (
    id BIGINT IDENTITY(1,1) PRIMARY KEY, member_id BIGINT NOT NULL REFERENCES user_accounts(id),
    copy_id BIGINT NOT NULL REFERENCES book_copies(id), requested_at DATETIME2 NOT NULL,
    issued_at DATETIME2 NULL, due_at DATETIME2 NULL, returned_at DATETIME2 NULL,
    status NVARCHAR(20) NOT NULL, renewal_count INT NOT NULL DEFAULT 0
);
CREATE TABLE library_events (
    id BIGINT IDENTITY(1,1) PRIMARY KEY, title NVARCHAR(180) NOT NULL, description NVARCHAR(2000) NOT NULL,
    location NVARCHAR(180) NOT NULL, start_at DATETIME2 NOT NULL, capacity INT NOT NULL, status NVARCHAR(20) NOT NULL
);
CREATE TABLE event_registrations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY, event_id BIGINT NOT NULL REFERENCES library_events(id),
    member_id BIGINT NOT NULL REFERENCES user_accounts(id), registered_at DATETIME2 NOT NULL,
    attended BIT NOT NULL DEFAULT 0, CONSTRAINT uq_event_member UNIQUE(event_id,member_id)
);
CREATE TABLE feedback_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY, member_id BIGINT NOT NULL REFERENCES user_accounts(id),
    type NVARCHAR(20) NOT NULL, subject NVARCHAR(180) NOT NULL, message NVARCHAR(2500) NOT NULL,
    status NVARCHAR(20) NOT NULL, response NVARCHAR(2500) NULL, created_at DATETIME2 NOT NULL, resolved_at DATETIME2 NULL
);
CREATE TABLE fine_payments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY, loan_id BIGINT NOT NULL UNIQUE REFERENCES loans(id),
    member_id BIGINT NOT NULL REFERENCES user_accounts(id), amount DECIMAL(10,2) NOT NULL,
    status NVARCHAR(20) NOT NULL, paid_at DATETIME2 NULL, reference NVARCHAR(80) NULL
);
CREATE TABLE notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY, recipient_id BIGINT NOT NULL REFERENCES user_accounts(id),
    title NVARCHAR(180) NOT NULL, message NVARCHAR(1000) NOT NULL, type NVARCHAR(20) NOT NULL,
    is_read BIT NOT NULL DEFAULT 0, created_at DATETIME2 NOT NULL
);
CREATE TABLE audit_logs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY, actor NVARCHAR(180) NOT NULL, action NVARCHAR(80) NOT NULL,
    entity_type NVARCHAR(80) NOT NULL, entity_id BIGINT NULL, details NVARCHAR(1000) NULL, occurred_at DATETIME2 NOT NULL
);
CREATE TABLE reading_list_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY, member_id BIGINT NOT NULL REFERENCES user_accounts(id),
    book_id BIGINT NOT NULL REFERENCES books(id), created_at DATETIME2 NOT NULL,
    CONSTRAINT uq_reading_member_book UNIQUE(member_id,book_id)
);
CREATE INDEX ix_loans_status_due ON loans(status,due_at);
CREATE INDEX ix_notifications_recipient ON notifications(recipient_id,is_read);
