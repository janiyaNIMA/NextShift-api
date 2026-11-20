# UniShift / NextShift — Diagrams.net (Draw.io) Class Diagrams Guide
# https://app.diagrams.net/ මඟින් Class Diagrams විවෘත කිරීමේ මඟපෙන්වීම

ඔබට [https://app.diagrams.net/](https://app.diagrams.net/) (Draw.io) වෙබ් අඩවිය මඟින් UniShift backend හි සියලුම Class Diagrams ඉතා පැහැදිලිව සහ අලංකාරව (Modern UI Palette & UML Notation) නැරඹීමට සහ Edit කිරීමට අවශ්‍ය `.drawio` ගොනු මෙහි සකසා ඇත.

---

## 1. විවෘත කරන්නේ කෙසේද? (How to Open in app.diagrams.net)

1. වෙබ් බ්‍රව්සරයෙන් **[https://app.diagrams.net/](https://app.diagrams.net/)** වෙත පිවිසෙන්න.
2. මුල් පිටුවේදී **"Open Existing Diagram"** (හෝ File $\rightarrow$ Open From $\rightarrow$ Device) තෝරන්න.
3. මෙම project එකේ ඇති **[docs/unishift_class_diagrams.drawio](file:///home/janidu/workbench/unishift-backend/docs/unishift_class_diagrams.drawio)** ගොනුව තෝරා Open කරන්න (නැතහොත් ගොනුව browser window එකට Drag & Drop කරන්න).
4. Diagrams.net හි පහළින් (bottom tab bar) පිටු (Pages / Tabs) 5 ක් දිස්වනු ඇත:
   - **Tab 1:** `1. Domain Entities & OOP Hierarchy`
   - **Tab 2:** `2. Strategy Pattern & Polymorphism`
   - **Tab 3:** `3. Complete 3-Tier Layered Architecture`
   - **Tab 4:** `4. Exception Hierarchy & Error Handling`
   - **Tab 5:** `5. Security & DTO Records`

---

## 2. සකස් කර ඇති ගොනු ලැයිස්තුව (Available Draw.io Files)

### ප්‍රධාන Master File (සියලුම Diagrams ටැබ් 5කින් සමන්විතයි):
- 📁 **[unishift_class_diagrams.drawio](file:///home/janidu/workbench/unishift-backend/docs/unishift_class_diagrams.drawio)**

### වෙන වෙනම ලබාගෙන ඇති Individual Diagrams (`docs/drawio/`):
| අංකය | ගොනුව (File Link) | අන්තර්ගතය සහ OOP මූලධර්මය |
|---|---|---|
| **01** | [1_domain_entities_and_oop.drawio](file:///home/janidu/workbench/unishift-backend/docs/drawio/1_domain_entities_and_oop.drawio) | `BaseEntity`, `User`, `Job`, `Booking`, Enums, `extends` Inheritance, Encapsulation, Associations |
| **02** | [2_recommendation_strategy_pattern.drawio](file:///home/janidu/workbench/unishift-backend/docs/drawio/2_recommendation_strategy_pattern.drawio) | `JobRecommendationStrategy` (Interface), `AllPublishedJobsStrategy`, Future ML/Proximity strategies, Polymorphism, DIP, OCP |
| **03** | [3_complete_system_architecture.drawio](file:///home/janidu/workbench/unishift-backend/docs/drawio/3_complete_system_architecture.drawio) | Controllers $\rightarrow$ Services $\rightarrow$ Repositories $\rightarrow$ Database සම්පූර්ණ 3-Tier Layered System Architecture එක |
| **04** | [4_exception_hierarchy.drawio](file:///home/janidu/workbench/unishift-backend/docs/drawio/4_exception_hierarchy.drawio) | `ApiException` Superclass එකෙන් `ConflictException`, `ResourceNotFoundException` ආදී exceptions inherit වන ආකාරය සහ `ApiExceptionHandler` |
| **05** | [5_security_and_dtos.drawio](file:///home/janidu/workbench/unishift-backend/docs/drawio/5_security_and_dtos.drawio) | `UserDetails` $\leftarrow$ `UserPrincipal`, `JwtAuthenticationFilter`, Immutable Java 21 `record` DTOs |

---

## 3. Diagrams වල විශේෂතා (Visual Design Features)

- **Standard UML Arrows**:
  - `extends` (Generalization / Class Inheritance): Closed hollow triangle arrow (`▲`).
  - `implements` (Realization / Interface Implementation): Dashed line with closed hollow triangle (`△`).
  - `association`: Solid directed arrow (`→`) with multiplicity labels (`[1]`, `[0..*]`).
- **Modern Color Palette (Tailwind-inspired)**:
  - **Emerald Green**: Domain Entities & BaseEntity (`#ecfdf5`, `#10b981`)
  - **Amber Yellow**: Domain Enumerations (`#fef3c7`, `#f59e0b`)
  - **Warm Orange**: Strategy Pattern & Abstract Interfaces (`#fff7ed`, `#ea580c`)
  - **Sky Blue**: Application Services (`#e0f2fe`, `#0284c7`)
  - **Purple / Indigo**: REST Controllers (`#ede9fe`, `#8b5cf6`)
  - **Rose Red**: Custom Exception Hierarchy (`#ffe4e6`, `#f43f5e`)
  - **Slate / Teal**: DTO Records & Security Components
- **Exporting to High-Resolution Images / PDF**:
  - Draw.io හි `File` $\rightarrow$ `Export as` $\rightarrow$ `PNG` (Transparent Background, 300 DPI) හෝ `PDF` තේරීමෙන් Presentation slides හෝ Viva reports සඳහා ඉතා පැහැදිලි රූප ලබාගත හැක.
