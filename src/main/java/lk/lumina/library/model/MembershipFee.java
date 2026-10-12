package lk.lumina.library.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="membership_fees")
public class MembershipFee {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(optional=false) @JoinColumn(name="member_id",unique=true) private UserAccount member;
    @Column(nullable=false,precision=10,scale=2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentStatus status=PaymentStatus.PAID;
    @Column(nullable=false) private LocalDateTime paidAt=LocalDateTime.now();
    @Column(nullable=false,length=80) private String reference;
    public MembershipFee(){}
    public MembershipFee(UserAccount member,BigDecimal amount,String reference){this.member=member;this.amount=amount;this.reference=reference;}
}
