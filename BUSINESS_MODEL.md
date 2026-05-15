# 💼 DysphagiaGuard — Business Model & Commercial Strategy

> *Version 1.0 — EthAum Venture Partners Healthcare MVP Submission*

---

## 1. Executive Summary

DysphagiaGuard is a **B2B2C medical IoT company** commercializing continuous swallowing disorder monitoring. We replace episodic, expensive clinical assessment (₹8,000–25,000/session) with a **₹1,000 hardware device + software subscription** that works 24/7, at the bedside or at home.

**The ask from EthAum**: Seed funding + accelerator support to complete clinical validation and achieve first 10 hospital contracts.

---

## 2. Market Opportunity

### Total Addressable Market (TAM)

| Segment | India Patients | Global Patients | Market Size (India) |
|---|---|---|---|
| Stroke survivors with dysphagia | ~4 million | ~80 million | ₹1,200 Cr |
| Parkinson's Disease | ~1.5 million | ~10 million | ₹450 Cr |
| ALS / Motor Neuron Disease | ~100,000 | ~450,000 | ₹80 Cr |
| ICU / post-surgical dysphagia | ~2 million/yr | — | ₹600 Cr |
| **Total TAM (India)** | **~8 million** | | **~₹2,400 Cr** |

### Serviceable Addressable Market (SAM) — Year 1–3 Focus

- **Tier 1 hospitals** in Bangalore, Chennai, Mumbai, Delhi with neurology/ICU wards
- **Elder care homes** (India has ~2,000+ registered facilities; growing 18% YoY)
- **Home caregivers** of post-stroke patients discharged from hospitals

**SAM (3-year)**: ₹240 Cr (10% penetration of hospital + institutional segment)

### Why Now?

1. Post-COVID telemedicine normalization — hospitals actively seeking remote monitoring tools
2. NHM and Ayushman Bharat pushing digital health infrastructure
3. ESP32 + commodity sensor costs have reached sub-₹500 for BOM, enabling ₹999 consumer pricing
4. No comparable continuous-monitoring competitor exists in India today

---

## 3. Competitive Landscape

| Solution | Type | Price | Continuous? | India Available? | DysphagiaGuard Advantage |
|---|---|---|---|---|---|
| Videofluoroscopy (VFSS) | Clinical procedure | ₹8,000–25,000 | ❌ Episodic | ✅ | 10x cheaper, continuous |
| FEES (Endoscopy) | Clinical procedure | ₹5,000–15,000 | ❌ Episodic | ✅ | Non-invasive |
| Biofeedback surface EMG | Research device | $5,000–15,000 | Partial | ❌ | Neck-worn, BLE, affordable |
| Swallowing apps (Logosmart) | Mobile app only | $0–50/mo | ❌ Passive | ❌ | Hardware + AI detection |
| **DysphagiaGuard** | **IoT + SaaS** | **₹999–4,999/mo** | **✅ Real-time** | **✅** | **Full stack, India-first** |

**Moat**: We are the only real-time continuous multi-sensor swallowing classifier available in India at consumer-accessible price points, with a growing proprietary swallowing event dataset.

---

## 4. Business Model

### Revenue Streams

#### 🏥 Tier 1 — Hospital & ICU (B2B Enterprise)

**Model**: Device lease + monthly SaaS subscription per ward

| Component | Price |
|---|---|
| Device (ESP32 wearable, 5-unit ward pack) | ₹0 upfront (lease) |
| Ward monitoring dashboard license | ₹4,999 / month / ward |
| Setup & onboarding | ₹5,000 one-time |
| SLP clinical report API access | Included |

**Target customers**: Neurology ICUs, stroke rehab wards, post-surgical ICUs  
**Sales motion**: Direct enterprise sales to procurement/CMO; pilot with KPI outcomes

#### 👵 Tier 2 — Elder Care Homes (B2B SME)

**Model**: Per-patient monthly subscription, hardware sold

| Component | Price |
|---|---|
| Device (per patient) | ₹3,999 one-time |
| App subscription | ₹999 / patient / month |
| Multi-patient dashboard | ₹2,999 / facility / month |

**Target customers**: Nursing homes, assisted living facilities, palliative care centers  
**Sales motion**: Regional distributor network; partner with elder care chains

#### 🏠 Tier 3 — Home Caregiver (B2C)

**Model**: Hardware purchase + app subscription

| Component | Price |
|---|---|
| DysphagiaGuard device (retail) | ₹7,999 |
| App subscription (Basic) | ₹299 / month |
| App subscription (Pro — AI + PDF + SMS) | ₹599 / month |
| Caregiver family pack (3 months) | ₹1,499 |

**Target customers**: Families of stroke/Parkinson's patients discharged from hospitals  
**Sales motion**: Hospital discharge referrals, neurologist prescriptions, Amazon/Flipkart

### Unit Economics (Tier 2 Example)

```
Revenue per patient/month    : ₹999
COGS (amortized hardware)    : ₹167  (₹3,999 device / 24 months)
Cloud + infra                : ₹30
Support                      : ₹50
Gross Margin per patient/mo  : ~₹752  (~75%)

Break-even per customer      : Month 1 (subscription > COGS after hardware recovery)
```

---

## 5. Go-To-Market Strategy

### Phase 1 — Beachhead (Months 1–6): Bangalore Hospital Pilots

**Goal**: 3 hospital pilot contracts, 50 active patients, first clinical validation data

**Actions**:
- Partner with 1–2 neurology/rehab departments in Bangalore (St. John's, Manipal, Fortis)
- Offer 3-month free pilot in exchange for IRB-approved data collection
- Assign clinical champion (Speech-Language Pathologist) as internal advocate
- Collect sensitivity/specificity data against VFSS gold standard

**KPIs**:
- ≥85% detection sensitivity for unsafe swallows
- ≥3 NPS score ≥8 from nursing staff
- 0 device-related adverse events

### Phase 2 — Expansion (Months 7–12): Elder Care + Home

**Goal**: 500 active paying subscribers, ₹25L ARR

**Actions**:
- Publish clinical pilot outcomes as a preprint / conference poster (ISCH 2025)
- Launch on Amazon/Flipkart with neurologist endorsements
- Partner with 5 elder care home chains in South India
- Build SLP referral program: ₹500 commission per patient activation

### Phase 3 — Scale (Year 2): Platform + Regulatory

**Goal**: 5,000 active patients, ₹2.5 Cr ARR, CDSCO Class B certification

**Actions**:
- Launch multi-patient hospital dashboard (ward management)
- Integrate with major EHR platforms (HL7 FHIR API)
- Begin CDSCO Class B Medical Device registration
- Explore Series A with healthcare VCs

---

## 6. Network / Flywheel Effect

```
More patients wearing DysphagiaGuard
          │
          ▼
Larger proprietary swallowing event dataset
          │
          ▼
Better ML model accuracy (personalized baselines)
          │
          ▼
Better clinical outcomes → more hospital endorsements
          │
          ▼
More hospital referrals → more patients
          │
          └──────────────── (flywheel repeats) ────────────────┐
                                                               │
                 Data moat grows, competition can't replicate  │
                                          ◄────────────────────┘
```

**Data is the moat**: Every patient generates labeled swallowing event data (safe/unsafe/cough + patient outcome). After 10,000+ labeled events, the adaptive ML model becomes significantly more accurate than any rule-based threshold system — creating a defensible competitive advantage that grows with scale.

**Network effect (Tier 1)**: Hospitals that use DysphagiaGuard generate PDF reports that SLPs share with other hospitals, driving organic B2B referrals.

---

## 7. Financial Projections

### 3-Year Revenue Model

| Year | Active Patients | Avg Revenue/Patient/Mo | ARR |
|---|---|---|---|
| Year 1 | 200 | ₹850 | **₹20.4 L** |
| Year 2 | 1,500 | ₹900 | **₹1.62 Cr** |
| Year 3 | 6,000 | ₹950 | **₹6.84 Cr** |

### Cost Structure (Year 1)

| Category | Monthly Cost |
|---|---|
| Hardware manufacturing (BOM + assembly) | ₹40,000 |
| Cloud infrastructure (AWS/Firebase) | ₹8,000 |
| Clinical trial / regulatory consulting | ₹25,000 |
| Sales & BD | ₹30,000 |
| Team (2 founders + 1 SLP advisor) | ₹80,000 |
| **Total Monthly Burn** | **~₹1.83 L** |

**Runway with ₹50L seed**: ~27 months

---

## 8. Funding Ask & Use of Funds

### EthAum Seed Round: ₹25–50 Lakhs

| Allocation | % | Amount |
|---|---|---|
| Clinical pilot + IRB study | 30% | ₹7.5–15L |
| Hardware manufacturing (500 units) | 25% | ₹6–12.5L |
| Android app v2.0 (ML upgrade) | 20% | ₹5–10L |
| CDSCO regulatory filing | 15% | ₹3.75–7.5L |
| Sales & BD (hospital outreach) | 10% | ₹2.5–5L |

---

## 9. Risk Analysis & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Clinical validation fails (low sensitivity) | Medium | High | Iterative threshold tuning; TFLite ML upgrade in Phase 3 |
| Regulatory delays (CDSCO) | High | Medium | Early pre-submission consultation; operate as research device initially |
| Hospital procurement cycles too long | High | Medium | Start with elder care (faster B2B sales cycle) |
| Competitor launches similar product | Low | High | Proprietary dataset moat; first-mover brand with SLP community |
| Hardware reliability in field | Medium | High | Accelerated aging tests; 6-month warranty; field-replace program |

---

## 10. Why EthAum + DysphagiaGuard?

EthAum's portfolio and network in healthcare deep tech + Singapore/India market access is a perfect match for our Phase 2 expansion. Specifically:

- **Clinical network**: EthAum's hospital connections accelerate our pilot program by 6–12 months
- **Regulatory expertise**: CDSCO + CE Mark guidance from portfolio companies
- **Revenue share model**: Aligns with EthAum's 25% revenue share structure — we're motivated to scale ARR, not just raise
- **Southeast Asia expansion**: Post-India product-market fit, Singapore + Thailand have similar elder care demographics and no local solutions

---

*DysphagiaGuard — Built for EthAum Venture Partners Healthcare MVP Competition*  
*Team: Ashwinkumar K + Dhamarai | Chennai Institute of Technology, Bangalore*  
*Contact: [Your email] | GitHub: github.com/Ashwinkumar-k10/Dysphagia_Guard*
