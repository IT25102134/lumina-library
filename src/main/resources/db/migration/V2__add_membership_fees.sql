CREATE TABLE membership_fees (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE REFERENCES user_accounts(id),
    amount DECIMAL(10,2) NOT NULL,
    status NVARCHAR(20) NOT NULL,
    paid_at DATETIME2 NOT NULL,
    reference NVARCHAR(80) NOT NULL
);
