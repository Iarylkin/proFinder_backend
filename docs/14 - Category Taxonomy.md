# ProFinder — Category Taxonomy

- **Version:** 1.2
- **Date:** 2026-10-27
- **Status:** Stable
- **Purpose:** The concrete list of service categories to seed the `categories` table with at launch. The model (flat list, admin-managed CRUD) is defined in [6 - Database Schema.md](6%20-%20Database%20Schema.md) and [7 - Application Classes.md](7%20-%20Application%20Classes.md); this file supplies the **data**. [Documentation Roadmap.md](Documentation%20Roadmap.md) Tier 2 "Category taxonomy — seed set".

**Design constraints (already decided):**
- **Flat list, no hierarchy** — no `parent_id`, no parent/leaf matching ([A4](Documentation%20Roadmap.md)). An order's `category_id` matches a professional only on exact equality.
- Categories are **predefined by admins** (a `SENIOR` moderator, [11 § 4](11%20-%20Permission%20Matrix.md)); users pick from the list, they don't create categories.
- `categories.name` is `UNIQUE` ([6](6%20-%20Database%20Schema.md)). Comparison is case-insensitive per [13 § 8](13%20-%20Validation%20Rules.md).

---

## 1. Naming conventions
---

- **Title Case**, singular-trade or gerund form as it reads naturally ("Plumbing", "Appliance Repair", "House Cleaning").
- Broad enough that most professionals fit **one to three** categories; narrow enough to be a useful search filter.
- No location, no skill level, no price tier in the name.
- ASCII only in MVP (English-only, [NFR-UX-4](2%20-%20Requirements.md)).


## 2. Seed list (32 categories)
---

Grouped here **only for readability** — the groups are not stored, there is no group column.

### Trades & repair
---

| Name | Typical work |
|---|---|
| Plumbing | Leaks, fixtures, pipes, water heaters |
| Electrical | Wiring, outlets, fixtures, panels |
| HVAC & Heating | AC, furnaces, ventilation, thermostats |
| Appliance Repair | Fridges, washers, dryers, ovens, dishwashers |
| Handyman | Small mixed jobs, mounting, assembly, minor repairs |
| Carpentry | Custom woodwork, framing, trim, shelving |
| Locksmith | Locks, keys, security hardware |

### Home improvement & renovation
---

| Name | Typical work |
|---|---|
| Painting & Decorating | Interior/exterior painting, wallpaper |
| Flooring | Hardwood, laminate, tile, vinyl, carpet |
| Tiling | Walls, floors, backsplashes, regrouting |
| Roofing | Repairs, replacement, gutters |
| Drywall & Plastering | Hanging, patching, skim coating |
| Kitchen & Bath Remodeling | Full room renovations |
| Windows & Doors | Installation, replacement, weatherproofing |
| Insulation | Attic, wall, soundproofing |
| Masonry & Concrete | Brick, block, patios, driveways, walkways |

### Outdoor & seasonal
---

| Name | Typical work |
|---|---|
| Landscaping & Gardening | Design, planting, maintenance |
| Lawn Care | Mowing, fertilizing, aeration |
| Tree Services | Trimming, removal, stump grinding |
| Fencing & Decking | Build, repair, staining |
| Pool & Spa Services | Cleaning, opening/closing, repairs |
| Snow Removal | Driveways, walkways, seasonal contracts |
| Pressure Washing | Siding, driveways, decks |

### Cleaning & maintenance
---

| Name | Typical work |
|---|---|
| House Cleaning | Regular, deep, move-in/move-out |
| Carpet & Upholstery Cleaning | Steam cleaning, stain removal |
| Window Cleaning | Interior/exterior, screens |
| Pest Control | Inspection, treatment, prevention |
| Junk Removal & Hauling | Cleanouts, disposal, single items |

### Moving & installation
---

| Name | Typical work |
|---|---|
| Moving & Packing | Local moves, loading, packing services |
| Furniture Assembly | Flat-pack, built-ins |
| TV & Home Theater Mounting | Wall mounting, wiring, setup |
| Smart Home Installation | Cameras, thermostats, doorbells, hubs |

*(Count check: 7 + 9 + 7 + 5 + 4 = 32, matching the `INSERT` in §3.)*


## 3. Seed as data
---

For the Flyway migration (`V00X__seed_categories.sql`) — order doesn't matter, `name` is the natural key:

```sql
INSERT INTO categories (id, name, created_at, updated_at) VALUES
  ('01a17aa8-1c01-70a0-abeb-676af6eb0f59', 'Plumbing',                    now(), now()),
  ('01a17aa8-1c02-7ebc-b160-3e5c0bb6d7e6', 'Electrical',                  now(), now()),
  ('01a17aa8-1c03-72aa-9cd8-f25498342957', 'HVAC & Heating',              now(), now()),
  ('01a17aa8-1c04-7cda-96f8-3f4538f19d3c', 'Appliance Repair',            now(), now()),
  ('01a17aa8-1c05-7315-9159-ceaeff739936', 'Handyman',                    now(), now()),
  ('01a17aa8-1c06-7494-b30c-3ea681788a76', 'Carpentry',                   now(), now()),
  ('01a17aa8-1c07-79ec-bf43-672a9dfdb665', 'Locksmith',                   now(), now()),
  ('01a17aa8-1c08-7839-862b-c10aa72149d7', 'Painting & Decorating',       now(), now()),
  ('01a17aa8-1c09-7cce-a0ef-48aa7f92d76f', 'Flooring',                    now(), now()),
  ('01a17aa8-1c0a-72a3-af80-779e009291ff', 'Tiling',                      now(), now()),
  ('01a17aa8-1c0b-714d-b079-f5e099732d2f', 'Roofing',                     now(), now()),
  ('01a17aa8-1c0c-764e-92d8-eb98edd59198', 'Drywall & Plastering',        now(), now()),
  ('01a17aa8-1c0d-7abd-90e2-d80dc2d476a8', 'Kitchen & Bath Remodeling',   now(), now()),
  ('01a17aa8-1c0e-7521-aadd-afd71e28a759', 'Windows & Doors',             now(), now()),
  ('01a17aa8-1c0f-7af8-bad7-0d060048f592', 'Insulation',                  now(), now()),
  ('01a17aa8-1c10-70c8-b37d-60208729cf3b', 'Masonry & Concrete',          now(), now()),
  ('01a17aa8-1c11-7a3f-bc21-f11853cbfd9a', 'Landscaping & Gardening',     now(), now()),
  ('01a17aa8-1c12-7ac4-a476-4b366a8fae7c', 'Lawn Care',                   now(), now()),
  ('01a17aa8-1c13-73e6-a33d-f9bb16e90d7f', 'Tree Services',               now(), now()),
  ('01a17aa8-1c14-78d3-b814-b77a63de5267', 'Fencing & Decking',           now(), now()),
  ('01a17aa8-1c15-7a4a-b3ed-de1131921285', 'Pool & Spa Services',         now(), now()),
  ('01a17aa8-1c16-7284-a50d-55ba9e68ee02', 'Snow Removal',                now(), now()),
  ('01a17aa8-1c17-7b87-9376-18b68524fd29', 'Pressure Washing',            now(), now()),
  ('01a17aa8-1c18-7025-b7ac-45608028a285', 'House Cleaning',              now(), now()),
  ('01a17aa8-1c19-7bf8-bf1c-1595d699c107', 'Carpet & Upholstery Cleaning',now(), now()),
  ('01a17aa8-1c1a-7af6-aa97-4751e42b8fee', 'Window Cleaning',             now(), now()),
  ('01a17aa8-1c1b-7640-b887-a0247efd77b2', 'Pest Control',                now(), now()),
  ('01a17aa8-1c1c-7a8a-b436-ef02a4e71518', 'Junk Removal & Hauling',      now(), now()),
  ('01a17aa8-1c1d-711d-9178-0534447bc165', 'Moving & Packing',            now(), now()),
  ('01a17aa8-1c1e-728b-bb48-204181b77446', 'Furniture Assembly',          now(), now()),
  ('01a17aa8-1c1f-743c-a0a7-17ad7ad5361e', 'TV & Home Theater Mounting',  now(), now()),
  ('01a17aa8-1c20-71bc-97eb-7baf5588f137', 'Smart Home Installation',     now(), now());
```

The ids are fixed **UUID v7** literals (time-ordered, generated once), not `gen_random_uuid()`: every id in the system is v7 and generated outside the database ([8 - API Specification.md § Conventions](8%20-%20API%20Specification.md)), and fixed literals give every environment and every test fixture the same category ids. Add new categories the same way, with a freshly generated v7.


## 4. Post-launch changes
---

- **Add** a category: `POST /categories` (`SENIOR` moderator). Immediately available as a filter and a selectable option.
- **Rename**: `PATCH /categories/{id}` — the `id` is stable, so professionals and orders keep their link; only the label changes. The professional and order ES documents carry `category_name`, so a rename requires a **reindex of every professional/order in that category**. *Implementation note:* the endpoint should enqueue that batch reindex.
- **Delete**: `DELETE /categories/{id}` — blocked (`409 CONFLICT`) while any order or professional still references it. In practice: reassign or wait it out.
- No merge/split operation in MVP.


## Changelog
---

| Version | Date | Change |
|---|---|---|
| 1.2 | 2026-10-27 | **D-1.4:** the seed `INSERT` uses fixed UUID v7 literals instead of `gen_random_uuid()`. |
| 1.1 | 2026-09-10 | Standardised to the shared doc format. Fixed the category count — the tables and the `INSERT` both hold **32**; the heading said 30 and the count-check said 31. Dropped "(Seed Set)" from the title and the stray `(proposed — note for the implementer)` marker. |
| 1.0 | 2026-09-07 | Initial. 32 seed categories with naming conventions, a Flyway `INSERT`, and post-launch add/rename/delete notes (incl. the rename→reindex consequence). |
