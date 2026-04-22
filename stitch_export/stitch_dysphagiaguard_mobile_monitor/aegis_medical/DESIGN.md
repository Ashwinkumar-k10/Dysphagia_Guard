# Design System Strategy: The Clinical Sentinel

## 1. Overview & Creative North Star
This design system is built upon the "Clinical Sentinel" Creative North Star. In a medical context, trust is not built through generic blue boxes, but through precision, authoritative hierarchy, and a sense of calm under pressure. 

We are departing from the "standard app" aesthetic by adopting an editorial-digital hybrid. This means leveraging aggressive typography scales, intentional asymmetry in data visualization, and a "Deep Space" depth model. While the system follows Material Design 3 logic, we are elevating it through sophisticated tonal layering and glassmorphism to create a UI that feels like a high-end medical instrument rather than a basic mobile utility.

## 2. Colors & Surface Philosophy
The palette is rooted in a deep, nocturnal foundation to minimize eye strain and maximize the "pop" of critical medical alerts.

### The Color Tokens
*   **Primary (`#4cd6fb`):** Our teal beacon. Used for success states and primary actions.
*   **Secondary (`#4ae183`):** The "Safe" indicator. Highly saturated to ensure immediate recognition.
*   **Tertiary (`#ffb77d`):** The "Warning" indicator for non-critical alerts.
*   **Error (`#ffb4ab`):** Reserved strictly for critical "Unsafe" medical data.

### The "No-Line" Rule
To achieve a premium editorial feel, **this design system prohibits 1px solid borders for sectioning.** Boundaries must be defined solely through background color shifts or tonal transitions. 
*   Use `surface_container_low` for large background sections.
*   Use `surface_container_high` for primary cards.
*   Use `surface_container_highest` for interactive elements.
This creates a seamless, "molded" look where the UI feels like a single cohesive piece of hardware.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. A `surface_container_lowest` card should sit inside a `surface_container_low` section to create "negative depth" (a recessed feel), while a `surface_container_highest` card on a `surface` background creates "positive depth" (a floating feel).

### The "Glass & Gradient" Rule
To move beyond "out-of-the-box" Material Design:
*   **Glassmorphism:** Floating action sheets or navigation bars must use `surface_container_highest` at 80% opacity with a `20px` backdrop blur.
*   **Signature Textures:** Main CTAs should not be flat. Apply a subtle linear gradient from `primary` to `primary_container` at a 135-degree angle to provide a "lit" appearance that implies interactivity.

## 3. Typography: The Editorial Scale
We use a dual-typeface system to balance clinical precision with high-end readability.

*   **Display & Headlines (Public Sans):** This is our "Authoritative" voice. Used for high-level metrics and screen titles. The wide apertures of Public Sans provide a modern, open feel that reduces anxiety.
*   **Body & Labels (Inter):** This is our "Functional" voice. Inter’s tall x-height is specifically chosen for reading dense medical data and labels on small screens.

**Hierarchy as Identity:** 
Use `display-lg` for critical status numbers (e.g., a swallow success rate). Pair it with a `label-sm` in all-caps with 5% letter spacing to create a "Dashboard Instrument" aesthetic.

## 4. Elevation & Depth
We eschew traditional shadows in favor of **Tonal Layering**.

### The Layering Principle
Depth is achieved by "stacking" surface tiers.
1.  **Level 0 (Base):** `surface_dim` or `surface`.
2.  **Level 1 (Sub-section):** `surface_container_low`.
3.  **Level 2 (Interactive Card):** `surface_container_high`.

### Ambient Shadows
When an element must float (like a critical modal), use an **Ambient Shadow**:
*   **Blur:** 32px to 48px.
*   **Opacity:** 8%.
*   **Color:** Use a tinted version of `primary` or `on_surface` rather than black to ensure the shadow feels like a natural lighting effect within the deep navy environment.

### The "Ghost Border" Fallback
If a border is required for accessibility in high-glare environments, use a **Ghost Border**: The `outline_variant` token at **15% opacity**. Never use 100% opaque lines.

## 5. Components

### Buttons
*   **Primary:** Gradient fill (`primary` to `primary_container`), 8dp corner radius. No border. Text is `on_primary` in `title-sm` weight.
*   **Secondary:** `surface_container_highest` fill with a `ghost border`.
*   **Tertiary:** Text-only using the `primary` color token, reserved for low-emphasis actions like "Cancel" or "View Details."

### Clinical Cards
*   **Standard Cards:** 12dp corner radius. Use `surface_container_high`.
*   **Hero/Status Cards:** 24dp corner radius. These should use a subtle glow effect using the `secondary` (Safe) or `error` (Unsafe) tokens to indicate patient status at a glance.
*   **No Dividers:** Separate list items within cards using `16dp` of vertical whitespace and a subtle shift to `surface_container_highest` on hover/press.

### Input Fields
*   **Style:** Filled containers using `surface_container_highest`. 
*   **Focus State:** A 2px bottom-only stroke using the `primary` teal token. This keeps the "No-Line" rule intact for the overall container while providing clear focus.

### Semantic Alerting (Critical)
*   **High-Contrast Banners:** For "Unsafe" alerts, use an edge-to-edge `error_container` with `on_error_container` text. This is the only time the "No-Line" rule is superseded by a full-color block to ensure immediate clinical intervention.

## 6. Do’s and Don’ts

### Do:
*   **Do** use `surface_bright` to highlight the most important piece of data on a screen.
*   **Do** use asymmetrical layouts (e.g., a large metric on the left, supporting data on the right) to create an editorial feel.
*   **Do** ensure all "Safe/Unsafe" icons are accompanied by text to maintain accessibility for color-blind practitioners.

### Don’t:
*   **Don’t** use pure black (#000000). It kills the depth of the deep navy palette.
*   **Don’t** use standard Material 3 shadows. They are too "heavy" for this high-end clinical aesthetic.
*   **Don’t** use dividers or lines to separate list items. Trust the spacing scale and tonal shifts to do the work.