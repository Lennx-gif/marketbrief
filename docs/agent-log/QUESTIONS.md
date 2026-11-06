# Agent Questions & Decisions Log

| Q# | Date asked | Question | Default if unanswered | Answer (date) | Status |
| --- | --- | --- | --- | --- | --- |
| Q0.1 | 2026-11-06 | Screenshot availability and observations | Reference screenshots provided in `Page Layout` | Device screenshots and mockups verified (2026-11-06) | Resolved |
| Q0.2 | 2026-11-06 | Journal and Calendar CSV import presence | Existing at branch head `f9cefde` | Verified on branch head (2026-11-06) | Resolved |
| Q0.3 | 2026-11-06 | Canonical branch workflow & PR target | Work on current feature branch, clean commits ready for PR into main | Approved default (2026-11-06) | Resolved |
| Q9.1 | 2026-11-06 | Missing prices display "-" and "+0.0%" when key missing or quote unavailable | Show "Add a Twelve Data key to see prices" or "No price yet" instead of dashes / green zero | Approved default (2026-11-06) | Resolved |
| Q9.2 | 2026-11-06 | Analysis screen shows 0.00 Neutral, 0 headlines | Investigate tagging & time window query in `BACKLOG.md`; do not block R2 layout | Approved default (2026-11-06) | Resolved |
| Q9.3 | 2026-11-06 | Feed header subtitle copy ("Global Financial Intelligence") | Keep until interests customization lands | Approved default (2026-11-06) | Resolved |
| Q9.4 | 2026-11-06 | Duplicate calculator headings ("Position Calculator" & "Position Sizer") | Keep single heading "Position Sizer"; make Journal a pill-shaped button in top bar matching app aesthetics | Approved with user note: pill-shaped Journal button (2026-11-06) | Resolved |
| Q1.1 | 2026-11-06 | Keep region (country) filter or categories only? | Keep behind filter icon and bottom sheet; show active region as subtitle | Approved default (2026-11-06) | Resolved |
| Q1.2 | 2026-11-06 | Keep source avatar strip? | Off by default, scrollable with content, setting to enable | Approved default (2026-11-06) | Resolved |
| Q1.3 | 2026-11-06 | Where does "Saved" category live? | First chip in CategoryFilterBar | Approved default (2026-11-06) | Resolved |
| Q3.1 | 2026-11-06 | What should About say about the platform? | Ad-free news reader with in-app reading & market tools for forex-first traders; attributions (NewsAPI, Twelve Data, Myfxbook) & disclaimers | Approved default (2026-11-06) | Resolved |
| Q3.2 | 2026-11-06 | Product name and repo link | "MarketBrief", link to GitHub repository | Approved default (2026-11-06) | Resolved |
| Q4.1 | 2026-11-06 | Additional display settings beyond Theme? | None added for now | Approved default (2026-11-06) | Resolved |
| Q5.1 | 2026-11-06 | Live market trail data format? | Scrolling ticker strip on Watchlist (optional on Feed) + cached price chips on news cards | Approved default (2026-11-06) | Resolved |
| Q5.2 | 2026-11-06 | Real-time streaming vs adaptive polling? | Adaptive polling every 2-5 min while visible, labelled "as of hh:mm" | Approved default (2026-11-06) | Resolved |
| Q5.3 | 2026-11-06 | Calendar CSV import removal & calendar/news sourcing? | Remove CSV import completely; implement financial news sourcing outlet given Forex Factory has no direct public API | Approved: remove import entirely, ensure financial news sourcing outlet (2026-11-06) | Resolved |
| Q5.4 | 2026-11-06 | Persisted watchlist selections? | Persist items, sort order, view mode, calendar impact filter, and expanded sections | Approved default (2026-11-06) | Resolved |
| Q6.1 | 2026-11-06 | Calculator risk sizing display? | Show both percent and cash amount together (one editable, one derived) with aligned baseline | Approved default (2026-11-06) | Resolved |
| Q7.1 | 2026-11-06 | Daily life entries format? | Trading-oriented daily log: plan followed, mood, energy, notes, lessons, tags | Approved default (2026-11-06) | Resolved |
| Q7.2 | 2026-11-06 | Account-wise journaling? | Unlimited accounts with type (LIVE, DEMO, PROP), currency, starting balance, risk rules | Approved default (2026-11-06) | Resolved |
| Q7.3 | 2026-11-06 | Setup management playbook? | Yes, playbook with checklists, tags, per-setup statistics | Approved default (2026-11-06) | Resolved |
| Q7.4 | 2026-11-06 | Journal screenshot attachments? | Yes, optional, saved in app storage | Approved default (2026-11-06) | Resolved |
| Q7.5 | 2026-11-06 | Journal entry point? | Pill-shaped button in Calculator top bar | Approved with user note (2026-11-06) | Resolved |
| Q7.6 | 2026-11-06 | Cloud backup? | Exclude journal from cloud backup (local only) | Approved default (2026-11-06) | Resolved |
| Q7.7 | 2026-11-06 | Migration of existing account/trades? | Migrate existing `account_settings` & trades to "Account 1" | Approved default (2026-11-06) | Resolved |
| Q8.1 | 2026-11-06 | In-app web fallback ad/tracker blocking? | Yes, block known ad/tracker hosts and hide common ad containers, with "Show full page" toggle | Approved default (2026-11-06) | Resolved |
| Q8.2 | 2026-11-06 | JavaScript enabled in fallback WebView? | Enabled, without JavaScript interfaces/bridges | Approved default (2026-11-06) | Resolved |
| Q8.3 | 2026-11-06 | Link navigation handling? | Same domain stays in WebView; external domain opens Custom Tab on tap | Approved default (2026-11-06) | Resolved |
