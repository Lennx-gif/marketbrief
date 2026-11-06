# Agent Backlog

Out-of-scope observations, follow-ups, and prospective improvements discovered during implementation:

1. **Analysis Sentiment 0.00 / 0 Headlines Investigation (Q9.2)**
   - Investigate why sentiment tagging or time-window query yielded 0.00 Neutral despite cached articles in Room. Verify tag matching logic in `core/analysis` or `ArticleRepository`.
2. **Extractor Tolerance and Resiliency**
   - Continuously expand article content heuristic parsers for edge-case publication structures to maximize Markdown extraction before hitting web fallback.
3. **Markets Hub Dedicated Tab**
   - In future phases, consider elevating Journal and Watchlist under an integrated Markets hub in primary bottom navigation.
