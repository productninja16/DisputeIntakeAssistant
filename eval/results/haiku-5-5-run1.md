# Eval report

- Client: anthropic
- Model: claude-haiku-5-5
- Run at: 2026-10-14T01:02:53.738107Z
- Cases: 30

## Accuracy

| Measure | Result |
|---|---|
| Category correct | 30/30 (100%) |
| Queue correct | 28/30 (93%) |
| Both correct | 28/30 (93%) |

A case with no valid suggestion counts as wrong.

## Validation

| Measure | Result |
|---|---|
| Valid on first try | 30/30 (100%) |
| Valid after one retry | 0/30 (0%) |
| No suggestion (failed twice) | 0/30 (0%) |

## Confusion (rows: expected category, columns: predicted)

| Expected | Predicted |
|---|---|
| UNAUTHORIZED_TRANSACTION | UNAUTHORIZED_TRANSACTION x5; |
| DUPLICATE_CHARGE | DUPLICATE_CHARGE x5; |
| MERCHANDISE_NOT_RECEIVED | MERCHANDISE_NOT_RECEIVED x5; |
| INCORRECT_AMOUNT | INCORRECT_AMOUNT x5; |
| SUBSCRIPTION_CANCELLED | SUBSCRIPTION_CANCELLED x5; |
| OTHER | OTHER x5; |

## Misses

- **I04**: expected INCORRECT_AMOUNT / BILLING_ERRORS, got INCORRECT_AMOUNT / MERCHANT_DISPUTES
  - "The amount on my statement is higher than the total I signed on the receipt at the restaurant."
  - reasoning: The customer states the posted charge of 153.00 exceeds the total they signed on the restaurant receipt, which matches an incorrect amount dispute. The receipt and tip details are needed to confirm the discrepancy before acting.
- **O04** (ambiguous label): expected OTHER / MERCHANT_DISPUTES, got OTHER / GENERAL_REVIEW
  - "I returned the shoes on 2 March. The shop confirmed they got them back but my refund has still not arrived."
  - reasoning: The customer describes a returned purchase with a refund not received, which is a credit-not-processed issue rather than any listed dispute type. The transaction should be verified against the merchant's refund records before choosing a dispute category.

## Missing-info (not scored; for reading)

- U01: Whether the customer's card was lost, stolen, or shared with anyone; Whether the customer has other recent unrecognized charges; Whether any subscription or free trial with this merchant could be the source; Card present or card-not-present status and any available authorization details
- U02: Date and time the card was reported lost or stolen; Confirmation the customer did not authorize or know about the purchase; Whether the card was physically present or the transaction was card-not-present; Any prior authorization or recurring relationship with Electronics Mart
- U03: Whether the customer still has the physical card in their possession; Whether the card or account details were shared or lost before the charge; Whether any other transactions from the same period are disputed; Transaction authorization method (chip, PIN, or card-not-present) if available
- U04: Whether the customer still has the physical card in their possession; Whether the card or account number was disclosed or stored with any merchant; Any other unrecognized transactions on the account in the same period; Whether the customer has been to City Fuel's location or nearby
- U05: Whether the customer still has the physical card in their possession; Whether the 450.00 charge was made in person or online and its location; Date the customer first noticed the charge and whether they reported the login alert to the bank; Whether the card or account credentials were reset after the suspicious login
- D01: Second matching transaction record (amount and posting date) to confirm the duplicate; Whether the customer has already contacted the merchant for a refund
- D02: Confirmation that a second Metro Grocery charge of 87.34 appears on the same day (the statement shows only one posting for 2026-03-01); Whether the duplicate posting is pending or settled; Whether the customer made two separate purchases or only one
- D03: Confirmation that the second Pizza Palace charge is a separate posted transaction with the same amount (28.75); Timestamp of the second charge; Whether the customer has already contacted the merchant for a refund
- D04: Second matching transaction record (date, amount, merchant reference) to confirm a duplicate exists; Whether the customer recognizes or made the purchase; Merchant order or receipt reference
- D05: Original charge date and amount for the same ticket; Whether a second posted transaction from Skyline Airlines appears on the statement; Whether the customer has contacted the merchant for a refund
- I01: Original quoted rate and booking confirmation; Itemized folio or receipt from the hotel; Whether taxes or fees were disclosed at booking
- I02: Pump receipt or photo of the display showing the authorized amount; Whether the customer authorized an amount greater than $40 or a pre-authorization hold was placed; Confirmation of the original fuel quantity and price per litre/gallon
- I03: Prior billing history confirming the usual 59.99 amount; Whether any fees, taxes, or add-ons were included in the 79.99 charge; Telecom plan terms and any recent rate change notices
- I04: Signed receipt total and the posted amount to confirm the discrepancy; Whether a tip was added after signing or the merchant altered the total; Any merchant correspondence or corrected receipt
- I05: Copy of the receipt showing the accepted 20% discount code; Original order total before discount and the amount the customer expected to pay; Whether the merchant has been contacted about the discount
- M01: Order confirmation or order date from the merchant; Tracking number and carrier status history; Whether the customer has contacted the merchant for a refund or replacement; Whether the 1299.00 amount matches the expected price of a laptop bag
- M02: Number of tickets purchased versus number received; Whether the customer contacted Ticket Hub to request the missing tickets; Event date and whether the event has already occurred; Ticket price per unit to determine the value of the undelivered portion
- M03: Carrier tracking number and delivery scan details; Whether the customer has contacted the merchant about the order; Delivery address on the order versus the customer's address
- M04: Expected delivery date and whether the item has shipped or has tracking information; Whether the customer has contacted the merchant and the dates of those messages; Whether the customer has requested a refund from the merchant or the marketplace's buyer protection process; Order confirmation or receipt showing the item and agreed price
- M05: Order confirmation or expected delivery date from the merchant; Whether the customer contacted the merchant and what response was received; Any tracking or shipping records from Home Furnish Co
- S01: Date and confirmation of the cancellation request; Whether the merchant's cancellation was processed and effective date; Any prior charges or refunds for StreamBox on this card
- S02: Copy or date of the August written cancellation request; Gym's cancellation policy and whether it was acknowledged; Billing history showing charges after the cancellation date
- S03: Whether the customer cancelled the trial before the charge posted; Date the free trial started and the terms shown at signup; Any cancellation request or confirmation records from the merchant; Whether the customer used the service after the trial period
- S04: Date the customer deleted their account and whether any cancellation confirmation exists; Whether the Study App subscription was cancelled separately from account deletion; Prior charges from this merchant and whether the 2026-03-01 charge is a recurring renewal; Whether the customer has contacted the merchant to request a refund or cancellation
- S05: Date the customer says auto-renew was turned off; Whether the customer has any cancellation confirmation or record; Merchant's cancellation and renewal policy and records of the subscription status; Whether the customer has contacted the merchant about the charge
- O01: Specific nature of the service complaint; Whether the customer is disputing the charge itself or only expressing dissatisfaction; Whether the customer attempted to resolve the issue directly with Corner Cafe
- O02: ATM deposit or dispense receipt or journal entry; Whether the ATM was a bank-owned or third-party machine; Confirmation of the ATM's cash-count or reconciliation record for 2026-03-01; Whether the customer attempted to contact the branch or ATM operator
- O03: Customer's actual account of the Daily Bakery charge (what they recall, whether they made or recognize it); Whether the customer has a prior relationship with this merchant or a recurring arrangement; Card status and any prior dispute history for this account
- O04: Return confirmation or receipt from the shop; Whether the refund was issued by the merchant and its date; Whether the 89.00 charge is still pending or settled as a credit; Merchant's refund reference number
- O05: Whether the customer or a household member made the Pet Supplies Co purchase; Whether the customer recognizes the 2026-03-01 transaction at all; Whether the card or account was shared with the husband; Whether the customer received any pet supplies or a receipt
