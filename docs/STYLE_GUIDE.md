# Ekho Explorer Style Guide

The single source of truth for colors, text, spacing, and shapes.

## The rule
Screens read styling from the theme, through `AppStyle` or `MaterialTheme`. Do not type a hex color, a font size, a font name, or a spacing number into a screen file. If a value you need doesn't exist, add it to the theme (see "Changing or adding a value").

## Where things live
All files are in `ui/theme/`.

| File | Holds |
|---|---|
| `Color.kt` | The palette and the Material color roles |
| `Type.kt` | Text styles |
| `Spacing.kt` | Spacing values |
| `Shape.kt` | Corner radii and Material shapes |
| `Theme.kt` | `EkhoExplorerTheme`, the light color scheme, and the extended colors (votes, carousel dots) |
| `AppStyle.kt` | Compatibility layer: `AppStyle.colors`, `.type`, `.dimens` |

## Using the theme
1. Wrap the app and every `@Preview` in `EkhoExplorerTheme`. `MainActivity` already wraps the navigation. A preview without the wrapper shows Material's default purple palette.
2. Read values with `AppStyle.colors`, `AppStyle.type`, and `AppStyle.dimens`. `colors` and `type` are composable getters, so use them only inside composable functions. `dimens` is plain values.
3. Material components (buttons, cards, text fields) pick up the theme on their own.
4. Reading `MaterialTheme` directly is also fine. `AppStyle` names are the team's shared vocabulary.

## Colors
| `AppStyle.colors` | Material role | Hex | Use |
|---|---|---|---|
| `background` | background | #FFFFFF | Screen and card backgrounds |
| `surface` | surface | #F1F3F1 | Search field, image and avatar placeholders, nav indicator |
| `textPrimary` | onSurface | #121512 | Main text |
| `textSecondary` | onSurfaceVariant | #5E665E | Hints, secondary text, inactive icons |
| `accent` | primary | #2E9E4F | Brand color: selected items, cursor, logo box |
| `onAccent` | onPrimary | #FFFFFF | Content placed on the accent color |
| `divider` | outline | #3A8C3A | Dividers (brand green, kept deliberately) |
| `upvote` | extended | #2E9E4F | Active upvote |
| `downvote` | extended | #D9534F | Active downvote |
| `dotActive` | extended | #FFFFFF | Selected carousel dot (sits on photos) |
| `dotInactive` | extended | #80FFFFFF (50% white) | Unselected carousel dot |

Reserved roles: `Color.kt` also defines the generated Material roles below. No screen uses them yet. Use them before inventing a new color.

| Role | Hex |
|---|---|
| secondary / onSecondary | #516351 / #FFFFFF |
| tertiary / onTertiary | #39656D / #FFFFFF |
| primaryContainer / onPrimaryContainer | #B7F1BA / #1C5129 |
| error / onError | #BA1A1A / #FFFFFF |
| surfaceVariant | #DDE5DA |
| outlineVariant | #C1C9BE |

## Typography
Font family: the system default. Every style sets size, weight, line height, and letter spacing explicitly, so text matches the original design.

| `AppStyle.type` | Source | Size | Weight | Line height | Use |
|---|---|---|---|---|---|
| `caption` | bodyMedium | 14sp | Normal | 20sp | Post captions |
| `captionAction` | labelLarge | 14sp | Medium | font default | "More" / "less" actions |
| `count` | labelLarge | 14sp | Medium | font default | Vote counts |
| `username` | titleSmall | 15sp | SemiBold | font default | Usernames |
| `navLabel` | labelMedium | 12sp | Medium | font default | Navigation labels |
| `searchText`, `searchHint` | `SearchTextStyle` | 15sp | Normal | font default | Search field text and hint |
| `logo` | `LogoTextStyle` | 18sp | Bold | font default | Logo placeholder |

Notes:
- `bodyLarge` is unchanged (16sp, 24sp line height, 0.5sp spacing). It is the default for text with no explicit style.
- `SearchTextStyle` and `LogoTextStyle` sit outside the Material scale because no role matches their size and weight.
- Styles contain no color. Apply a color where the text is used.
- Material roles not listed keep Material's defaults.

## Spacing, shapes, and sizes
Spacing (`AppStyle.dimens`, from `Spacing.kt`):

| Name | Value |
|---|---|
| `xs` | 4dp |
| `sm` | 8dp |
| `md` | 12dp |
| `lg` | 16dp |
| `xl` | 24dp |

Shapes (from `Shape.kt`):

| Name | Value | Notes |
|---|---|---|
| `radiusSm` | 8dp | Also Material's `small` shape |
| `radiusMd` | 12dp | Also Material's `medium` shape |
| `pill` | 100dp | A radius constant, not a Material shape. Compose caps a corner radius at half the shorter side, so any element up to 200dp tall renders fully rounded |

Other Material shape sizes keep Material's defaults.

Component sizes (in `AppDimens`, tied to specific components rather than the theme): `avatar` 40dp, `logo` 40dp, `searchHeight` 44dp, `icon` 22dp, `touchTarget` 48dp, `dot` 6dp, `bottomBar` 80dp.

## Changing or adding a value
1. Edit only the theme files. Never override a value inside a screen.
2. If the value is new, give it a name and a documented purpose.
3. Open a small PR and announce it in the team chat. Theme files are shared, and `AppStyle.kt` is edited by several people.
4. Update this guide in the same PR.
5. Build and compare screens before and after. A change should be visible only where you intended it.

## AppStyle and migration status
`AppStyle` is a compatibility layer. Its names are unchanged, but values now come from the shared theme. New code may use either `AppStyle` or `MaterialTheme`. Files migrate to the theme directly when their owners next work on them, and `AppStyle` is removed only when nothing uses it.

Known hardcoded literals (not yet migrated):

| File | Literal |
|---|---|
| `MapScreen.kt` | `16.dp` padding, `8.dp` spacing (same as `lg` and `sm`) |
| `AppBottomBar.kt` | `20.dp` icon padding, `5.dp` divider thickness |
| `SpeciesSearchScreen.kt` | `80.dp` size |

## Known limitations
- Light theme only. There is no dark mode and no dynamic color, so the app ignores the device's dark-mode setting.
- Default font only.

## Review checklist
Before opening a PR, check that these find nothing outside `ui/theme/`:

```bash
grep -rn "Color(0x" app/src/main | grep -v "/ui/theme/"
grep -rn "fontSize\|fontFamily" app/src/main | grep -v "/ui/theme/"
grep -rn "[0-9]\.dp\|[0-9]\.sp" app/src/main | grep -v "/ui/theme/"
```

The third currently lists the known literals above. Anything new is a violation.

## AI Assitance 
- Markdown generated with help of claude.ai
