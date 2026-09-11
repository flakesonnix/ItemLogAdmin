# Architecture — ItemLogAdmin

GUI that reads ItemLog's DB — no shared code, DB is contract.

```
Paper
  └─ ItemLogAdminPlugin
       ├─ DataSourceProvider — same DB as ItemLog
       ├─ QueryService + ItemLogQueryRepository — search/filter/paginate
       ├─ RestoreService — give items back, write to restorations
       └─ GuiManager — player list → event list → details → confirm
```

`/itemlog` opens player list (search name/UUID), then events (filter type/time), then preview + restore. Audit in `restorations`.
