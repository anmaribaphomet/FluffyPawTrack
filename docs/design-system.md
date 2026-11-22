# Fluffy PawTrack — Design System

Consolidated color palette and type scale derived from the Figma file
[Fluffy PawTrack](https://www.figma.com/design/wW2C6PzFMazd7DsElw6Q38/Fluffy-PawTrack).
The Figma file defines no variables or styles; every value below was consolidated
from hand-applied fills and text layers (21 screens, 393×852).

Units: `dp` for sizes, `sp` for text (Compose Multiplatform).

---

## 1. Color palette

### 1.1 Primitive tokens

| Token | Hex | Merged from (Figma) | Typical use |
|---|---|---|---|
| `brown-900` | `#633D14` | — | Primary text, icons, back chevron |
| `brown-600` | `#AA9885` | `#A99681` | Muted text on light backgrounds, dividers |
| `brown-300` | `#C5A67E` | `#DEA467` | Home cards ("house" shape) |
| `cream-50` | `#F8F3E7` | `#FAF6EA`, `#FAF2E0` | App background, header wave |
| `cream-100` | `#F0E2D2` | — | Input fields, secondary surfaces |
| `white` | `#FFFFFF` | — | Pill buttons, cards on colored backgrounds |
| `peach-100` | `#FFE3BB` | `#FFE0B9`, `#FFDDB9`, `#FAE9CC` | Light highlight surfaces |
| `peach-200` | `#FCD4A1` | `#F4BD82` | Secondary surfaces on Home |
| `peach-300` | `#FEBE99` | — | Home background |
| `orange-500` | `#F8AE45` | `#FFA03C`, `#FF9526` | Primary brand color, avatar, inactive nav circles |
| `orange-700` | `#FF8300` | `#F9850C` | Active nav tab, pressed state |
| `sage-200` | `#D9E1B2` | `#D8E0AE` | Bottom wave, light green surfaces |
| `sage-400` | `#BFC98D` | `#C4CD93` | Settings rows, green buttons |
| `sage-700` | `#7A8542` | — | Toggles, green outlines |
| `pink-400` | `#FF8F9E` | — | Accent icons (heart, favorites) |
| `neutral-300` | `#D9D9D9` | — | Image placeholders, disabled state |
| `neutral-900` | `#2D2D2D` | `#000000` | System dialogs (location permission) |

Discarded as non-brand or stray values: `#204B38` (logo asset), `#E9D77B`,
`#D4FF00`, `#8A38F5` (Figma selection/debug strokes), `#936055`.

### 1.2 Semantic tokens

| Semantic token | Primitive |
|---|---|
| `background` | `cream-50` |
| `background-home` | `peach-300` |
| `surface` | `white` |
| `surface-variant` | `cream-100` |
| `surface-card` | `brown-300` |
| `surface-settings` | `sage-400` |
| `primary` | `orange-500` |
| `primary-strong` | `orange-700` |
| `on-primary` | `brown-900` |
| `on-background` | `brown-900` |
| `on-surface` | `brown-900` |
| `on-surface-muted` | `brown-600` |
| `secondary` | `sage-400` |
| `on-secondary` | `brown-900` |
| `accent` | `pink-400` |
| `outline` | `sage-700` |
| `disabled` | `neutral-300` |

### 1.3 Contrast (WCAG 2.1)

AA requires 4.5:1 for normal text and 3:1 for large text (≥ 18.66 sp bold or ≥ 24 sp).

| Text / background | Ratio | Result |
|---|---|---|
| `brown-900` / `cream-50` | 8.59 | ✅ AA |
| `brown-900` / `white` | 9.51 | ✅ AA |
| `brown-900` / `peach-300` | 5.91 | ✅ AA |
| `brown-900` / `sage-400` | 5.41 | ✅ AA |
| `brown-900` / `orange-500` | 5.04 | ✅ AA |
| `brown-900` / `brown-300` | 4.13 | ⚠️ Large text only |
| `brown-900` / `orange-700` | 3.85 | ⚠️ Large text only |
| `white` / `sage-400` | 1.76 | ❌ Fails |
| `white` / `orange-500` | 1.89 | ❌ Fails |
| `white` / `brown-300` | 2.30 | ❌ Fails |
| `brown-600` / `cream-50` | 2.52 | ❌ Fails for text |

Rules derived from this table:

- Text on colored surfaces is always `brown-900`, never white. This changes the
  Settings screen, which uses white labels on `sage-400` in Figma.
- `brown-600` is for decorative elements and dividers, not for readable text.
- `surface-card` keeps `brown-300`. Because `brown-900` on it is 4.13:1, text placed
  directly on the card must qualify as WCAG large text: ≥ 24 sp at any weight, or
  ≥ 19 sp Bold (700). Use `title-card` or larger. SemiBold (600) does not count as bold.
  Smaller text (labels, buttons) goes on a `white` pill, where contrast is 9.51:1.
  The only exception is the Home card descriptions (see section 3).

---

## 2. Type scale

Single family: **Inter**. All other families in the Figma file
(Istok Web, Itim) are replaced by the closest Inter style below.

| Token | Size / line height | Weight | Merged from (Figma) | Use |
|---|---|---|---|---|
| `display` | 32 / 40 sp | SemiBold (600) | Inter 32 | Home greeting |
| `headline` | 24 / 32 sp | SemiBold (600) | Inter 24 (SemiBold, Regular) | Screen titles, modal titles |
| `title` | 20 / 28 sp | SemiBold (600) | Inter 20 | Section titles |
| `title-card` | 20 / 28 sp | Bold (700) | Inter 20 | Text placed directly on `surface-card` |
| `body-large` | 16 / 24 sp | SemiBold (600) | Inter 16, Istok Web Bold 16 | Settings rows, menu items, primary buttons |
| `body` | 14 / 20 sp | Regular (400) | Inter 13, 14, 15, Istok Web Bold 13 | Descriptions, card body text, addresses |
| `label` | 12 / 16 sp | SemiBold (600) | Inter 12 | Pill button labels, chips, filters |
| `helper` | 12 / 16 sp | Regular Italic (400) | Inter Light Italic 12 and 16, Medium Italic 10 | Placeholders, hints ("Sin comentarios") |
| `caption` | 10 / 14 sp | SemiBold (600) | Inter 5, 8, 9, 10, Itim 11 | Bottom nav labels, map pin labels, counters |

Notes:

- Minimum size is 10 sp. The Figma file uses 5 sp and 8 sp (map pins, nav labels);
  those are raised to `caption` for legibility.
- Light (300) is dropped: thin strokes on light peach/cream backgrounds lose contrast.
- Letter spacing is 0 for every token.

---

## 3. Exceptions and pending fixes

1. **Accepted exception:** the Home card descriptions ("Ayúdanos a encontrarle un
   hogar…", "Checa en el mapa…") stay at `body` size directly on `surface-card`,
   keeping the Figma layout. They do not meet WCAG AA contrast (4.13:1, needs 4.5:1
   at that size). This is a deliberate product decision; no other text may rely on it.
2. Figma typos to fix when the copy is implemented: "Patitas Calljeras",
   "dispositvo", "Guardas" (→ "Guardados"), "atentido" (→ "atendido").
