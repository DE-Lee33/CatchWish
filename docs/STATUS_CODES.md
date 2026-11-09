# 상태값 코드표

DB CHECK 제약 기준입니다. 상태값은 아래 1글자 코드만 사용합니다.
노션 "MVC 통합 규칙 > 11. 상태값 코드표"와 함께 관리합니다. 코드를 추가·변경하면 **두 곳 모두** 갱신하세요.

> ⚠️ 같은 글자도 테이블마다 의미가 다릅니다. 예) 좌석 `C` = 취소대기, 예매 `C` = 확정

| 테이블.컬럼 | 코드 | 의미 | 비고 |
|---|---|---|---|
| PERFORMANCE.APPROVAL_STATUS | P | 심사중 | 기본값 |
| | A | 승인 | |
| | R | 반려 | |
| SEAT.SEAT_STATUS | A | 예매가능 | 미사용 좌석 코드 없음 |
| | H | 선점중 | |
| | B | 예매완료 | |
| | C | 취소대기 | |
| BOOKING.BOOKING_STATUS | W | 선점(결제대기) | ⚠️ 의미 확인 필요, 만료 코드 없음 |
| | C | 확정 | |
| | X | 취소 | |
| PAYMENT.PAYMENT_STATUS | P | 결제대기 | 취소 코드 없음 |
| | C | 결제완료 | |
| | F | 결제실패 | |
| QUEUE.STATUS | W | 대기 | |
| | A | 입장차례 | |
| | E | 만료 | |
| NOTIFICATION.SEND_STATUS | N | 미발송 | 발송 실패 코드 없음 |
| | Y | 발송완료 | |
| CANCELED_TICKET_SCHEDULE.SCHEDULE_STATUS | P | 예정 | ⚠️ 의미 확인 필요 |
| | O | 오픈 | |
| | C | 종료 | |

## Y/N 컬럼

`Y` = 예, `N` = 아니오

- NOTIFICATION.IS_READ
- CHECK_AUTH_CODE.IS_VERIFIED
- SEAT_LAYOUT.IS_TEMPLATE
- QUEUE.IS_VERIFIED
