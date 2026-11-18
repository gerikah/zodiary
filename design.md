# Zodiary design

- **Status:** Proposed design baseline for the Android MVP
- **Prepared:** 2026-11-18
- **Purpose:** Translate the supplied screen references and source copy into a usable design direction. This is a design specification, not an implementation.

## Source priority

- The 11 PNGs in [screens/](screens/) are the visual reference for layout and visual tone. They are static mockups, and several content panels are placeholders rather than complete screen designs.
- [Zodiary v3 Copy Library & Content Data Flow.pdf](<Zodiary v3 Copy Library & Content Data Flow.pdf>) is the latest working draft for proposed MVP behavior, content, and data flow. It marks several items for confirmation.
- [Zodiary Brand Messaging Revised v2.pdf](<Zodiary_Brand_Messaging_Revised_v2 (1).pdf>) is the approved brand baseline. Where v3 offers newer wording, this document uses it as a recommendation and records the difference for confirmation before release.
- Product and architecture decisions not settled in those sources are marked as proposals or open decisions in [project.md](project.md).

## 1. Design intent

Zodiary is a calm place to write about real life, with astrology used as a reflective prompt. The interface should feel personal and thoughtful without presenting readings as facts, predictions, or professional advice.

The journal is the core experience. Reading cards, zodiac personalization, mood selection, and affirmations should lead naturally into writing and help users revisit their own entries.

### Design principles

1. **The user owns the story.** Astrology offers a perspective; the person decides what resonates.
2. **Keep the writing path clear.** On the home screen, the primary action is to write today’s page. Reading and affirmation content support that action.
3. **Use expressive copy selectively.** Use poetic language in headings and confirmations. Keep labels, form hints, and actions direct.
4. **Make privacy and save status trustworthy.** Say what is actually stored and whether it has synced. Do not imply stronger privacy or offline support than the implementation provides.
5. **Keep the product visually consistent.** Purple is the shared base. Sign and element accents can vary color, while navigation and interaction patterns stay the same.

## 2. Visual system

The values below are starting tokens sampled from the supplied PNGs. They should be finalized as accessible design tokens before implementation; screenshot colors alone do not establish contrast compliance.

| Role | Starting value | Use |
|---|---|---|
| Canvas | #F7F3FB | Main onboarding and app background |
| Brand purple | #5B45A8 | Primary surfaces, selected navigation, headings and emphasis |
| Deep purple | #433D7D | High-emphasis text, borders and dark gradient stops |
| Soft lavender | #E1D1FE | Selected chips and quiet content panels |
| Form lavender | #F0E9FD | Form cards and tinted secondary surfaces |
| White surface | #FFFFFF | Inputs and cards that need separation |
| Warm gold | #E3B866 | Small celestial accents and restrained highlights |
| Ink | #2C254C | Main body text on light surfaces |

### Typography

- Use an editorial serif for large screen headings and short emotional statements, matching the references.
- Use a highly legible sans serif for body text, forms, navigation, buttons, and metadata.
- Keep longer reading and journal text in the sans serif for comfortable reading and writing.
- Starting scale: display 32–36sp, screen heading 26–32sp, section heading 20–24sp, body 16sp, helper text at least 14sp, and button labels 15–16sp. Allow system font scaling without clipping.
- Select licensed fonts during implementation. The screenshots do not identify a font family.

### Layout and surfaces

- The screen references are approximately 390 × 840 px. Treat them as phone layout references, not fixed dimensions.
- Use consistent horizontal margins around 20–24dp. Respect system status and navigation insets.
- Onboarding and form pages should scroll on short screens. Keep the primary action visible when practical, but never cover the keyboard or required form fields.
- Use softly rounded cards and input containers. A starting radius of 16–24dp matches the references; use one small set of radii rather than a different shape per screen.
- Reserve deep purple for high-emphasis areas. Keep most reading and writing surfaces light to reduce visual fatigue.
- Use the warm gold as a small accent. Avoid using it for long text or as the only indicator of selection.
- Primary buttons may use the purple-to-warm gradient shown in the references, with a clear accessible label and sufficient text contrast. Secondary actions should use text or a quiet outlined treatment.

### Sign and element accents

The sign reveal may transition from the purple foundation into the selected sign’s accent gradient. This is a visual accent only; it must not change the app’s interaction structure or reading language.

Use a sign illustration as a focal point on the reveal screen. The Libra character in the supplied image is an example of the intended warmth and scale, not a requirement to make every screen mascot-led. Provide meaningful accessibility text when an illustration conveys the selected sign; mark decorative stars as decorative.

### Navigation

The supplied signed-in screens show a four-item bottom bar: Home, Journal, Calendar, and Profile. Preserve those destinations for the MVP. Give icons accessible labels; show selected state with both a background or color treatment and a non-color cue. Keep the bar out of onboarding and authentication flows.

## 3. Screen and interaction specifications

| Screen/reference | Purpose and content | Main interaction and notes |
|---|---|---|
| Splash — [welcome-1.png](screens/welcome-1.png) | Zodiary wordmark, “Dear universe, it’s me.” and “Your personal cosmic journal.” | Brief brand entry, then welcome. Respect reduced-motion settings. |
| Welcome — [welcome.png](screens/welcome.png) | “The universe is listening. What’s your story?” with “The stars inspire. You write.” | Primary action: “Begin your story”. The mockup also shows Skip; whether onboarding can be skipped is an open product decision. |
| Sign setup — [sign setup.png](<screens/sign setup.png>) | Birthday picker, “Let’s find your sign.” and helper text explaining why the birthday is requested. | “Discover my sign”. Do not retain the full birth date unless the product requires it; see the privacy decision in [project.md](project.md). |
| Sign reveal — [sign reveal.png](<screens/sign reveal.png>) | “You’re a [Sign]!” with a sign illustration and “Your Zodiary just took on your colors.” | Use v3 action “Explore my sign” and secondary action “Not quite right? Change my sign.” The screenshot’s “That’s my sign” is older copy. |
| Preferences — [preferences.png](screens/preferences.png) | Three groups: topics (Love, Career, Well-being, Personal growth); journaling focus (Reflect, Track moods, Keep memories, A bit of everything); optional reminder timing. | Multi-select chips with clear selected states. Each saved topic must affect the prompt pool. Reminder choices include Morning, Evening, and Not now. |
| Trust note — not pictured | “A note before we begin” with “The stars offer a perspective; you decide what resonates.” Include the v3 reminder that readings are for reflection, not prediction or professional advice. | Show before the first reading. Acknowledge and continue. |
| All set — not pictured | “Everything’s falling into place.” Confirm the sign and preferences are saved and the journal is ready for its first page. | Continue into the app or the account step, depending on the account-gating decision. |
| Register — [register.png](screens/register.png) | Create-account heading, email, password, and confirmation fields; explain the purpose of the journal. | Use clear validation, password visibility control, and email verification state. The screenshot’s username field and social sign-in buttons are unconfirmed; see [project.md](project.md). |
| Login — [login.png](screens/login.png) | Returning-user heading, email and password fields, forgot-password action. | Support sign in, password reset, and a route to account creation. Do not make “remember me” a second session system; rely on the selected authentication provider’s session behavior. |
| Home — [home.png](screens/home.png) | Time-of-day greeting; today’s reading for the sign; journal prompt; “Words to carry today”; reading attribution wherever provider content appears. | Main action: “Write today’s page”. Reading card opens the reading detail. Show lucky details only if returned by the provider or explicitly curated and labeled “Traditional associations”. |
| Reading detail — not pictured | “Today’s reading”, “A fresh perspective for [Sign]”, complete text, attribution link, and disclaimer. | Provide loading, cached, stale, and unavailable states. Never interrupt the first read with feedback. |
| Journal — [journal.png](screens/journal.png) | “Today’s page”, mood check-in, prompt and truthful source label, and writing area with “Dear universe, today I…” placeholder. | “Try another prompt”, “Save entry”, and clear save/sync status. Save failure must retain the draft. |
| Calendar — [calendar.png](screens/calendar.png) | Month history, saved journal days, moods, reading feedback, and a journaling streak. | MVP is history only. A selected day shows “Your reading · Your mood · Your reflection”. Do not show forecast or favorable/unfavorable dates in the MVP. |
| Profile — [profile.png](screens/profile.png) | The screenshot is an unfinished content placeholder. Use this destination for sign, editable preferences, reminder settings, and account actions. | Keep V2 trait feedback out of the MVP unless it is separately approved. |

### Prompt labels

Use the source label that matches how a prompt was selected:

- Reading theme actually matched: “Inspired by today’s reading”.
- Selected from a preference pool: “A prompt on [Love / Career / Well-being / Growth]”.
- Selected from the sign or element pool: “A prompt for [Sign]”.
- General fallback: “A thought to begin with”.

Never label a generic prompt as reading-inspired. “Try another prompt” cycles within the current priority level before moving to the next fallback level.

### Journal feedback and state

- Mood check-in: “How’s your inner universe?” Helper: “No right or wrong feelings. Just yours.”
- Save confirmation: “A moment, kept.”
- Save failure: “Your thoughts are still here. Let’s try saving again.” Keep the text on screen and preserve the draft.
- Empty journal: “Every story starts with a blank page.”
- Drafts and offline copy must only be shown when the app actually saves drafts locally and can retry sync.
- Reading feedback (“Did today’s reading fit?”) appears after an entry is saved or when the reading is revisited later that day. Options: “Right on”, “Sort of”, and “Not really”. Store it with the reading; never treat it as a personality trait.

## 4. Copy and content rules

- Use “Today’s reading” in the app. Reserve “horoscope” for store search or SEO copy.
- Use “Words to carry today” for affirmations.
- Keep “Dear universe…” as the journal ritual; reserve “Dear universe, it’s me.” for splash, welcome, and campaign contexts.
- Prefer “Well-being” over the older v2 “Wellness” label.
- Display “Reading from CosmyDay” with a visible link wherever provider reading data appears, including any share image.
- Under readings, show: “The stars offer a perspective; you decide what resonates.” The About screen can state that readings are for reflection, not prediction or professional advice.
- Do not state that a reading was written by Zodiary or by astrologers. Do not invent lucky values, predictions, or reading dates.
- Notifications may say “Your [Sign] reading for today is ready.” They must not include reading text or guilt users about missed days.
- Use a private-journal claim only after the chosen storage and access controls are in place. The exact privacy copy is tracked in project.md.

## 5. Accessibility and responsive behavior

- Support Android font scaling, screen readers, keyboard and switch navigation, and system contrast settings.
- Make interactive touch targets at least 48dp where practical. Keep focus order aligned with the screen’s reading order.
- Keep text/background contrast readable; do not rely on purple/gold or color alone to communicate selected state, mood, sync state, or errors.
- Give form fields persistent labels. Error messages should identify the field and explain how to fix the problem.
- Provide text descriptions for meaningful sign art. Decorative stars and flourishes should not add noise to screen-reader output.
- Ensure the journal editor remains usable with the software keyboard open and that users can reach Save without losing text.
- Keep animations short and optional. Follow reduced-motion preferences and avoid animation that delays access to content.

## 6. Design gaps to resolve before implementation

The supplied screens do not yet define the complete reading detail, empty/loading/error states, all-set/trust screens, calendar day detail, profile content, or the behavior of onboarding Skip. The Home, Journal, Calendar, and Profile images contain placeholder panels, so their final card content and spacing need a reviewed screen design.

The account screens include details not confirmed by the newer copy brief: username, social sign-in, and a remember-me checkbox. [project.md](project.md) records these conflicts, the account-gating recommendation, and the decisions needed before the screens should be treated as build-ready.
