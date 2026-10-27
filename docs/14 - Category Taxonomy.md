# ProFinder — Category Taxonomy

- **Version:** 1.1
- **Date:** 2026-09-10
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
  (gen_random_uuid(), 'Plumbing',                    now(), now()),
  (gen_random_uuid(), 'Electrical',                  now(), now()),
  (gen_random_uuid(), 'HVAC & Heating',              now(), now()),
  (gen_random_uuid(), 'Appliance Repair',            now(), now()),
  (gen_random_uuid(), 'Handyman',                    now(), now()),
  (gen_random_uuid(), 'Carpentry',                   now(), now()),
  (gen_random_uuid(), 'Locksmith',                   now(), now()),
  (gen_random_uuid(), 'Painting & Decorating',       now(), now()),
  (gen_random_uuid(), 'Flooring',                    now(), now()),
  (gen_random_uuid(), 'Tiling',                      now(), now()),
  (gen_random_uuid(), 'Roofing',                     now(), now()),
  (gen_random_uuid(), 'Drywall & Plastering',        now(), now()),
  (gen_random_uuid(), 'Kitchen & Bath Remodeling',   now(), now()),
  (gen_random_uuid(), 'Windows & Doors',             now(), now()),
  (gen_random_uuid(), 'Insulation',                  now(), now()),
  (gen_random_uuid(), 'Masonry & Concrete',          now(), now()),
  (gen_random_uuid(), 'Landscaping & Gardening',     now(), now()),
  (gen_random_uuid(), 'Lawn Care',                   now(), now()),
  (gen_random_uuid(), 'Tree Services',               now(), now()),
  (gen_random_uuid(), 'Fencing & Decking',           now(), now()),
  (gen_random_uuid(), 'Pool & Spa Services',         now(), now()),
  (gen_random_uuid(), 'Snow Removal',                now(), now()),
  (gen_random_uuid(), 'Pressure Washing',            now(), now()),
  (gen_random_uuid(), 'House Cleaning',              now(), now()),
  (gen_random_uuid(), 'Carpet & Upholstery Cleaning',now(), now()),
  (gen_random_uuid(), 'Window Cleaning',             now(), now()),
  (gen_random_uuid(), 'Pest Control',                now(), now()),
  (gen_random_uuid(), 'Junk Removal & Hauling',      now(), now()),
  (gen_random_uuid(), 'Moving & Packing',            now(), now()),
  (gen_random_uuid(), 'Furniture Assembly',          now(), now()),
  (gen_random_uuid(), 'TV & Home Theater Mounting',  now(), now()),
  (gen_random_uuid(), 'Smart Home Installation',     now(), now());
```

If UUIDs need to be stable/known for test fixtures, replace `gen_random_uuid()` with fixed literals — but since categories are referenced by `id` FK and never by a hard-coded UUID in code, generated is fine.


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
| 1.1 | 2026-09-10 | Standardised to the shared doc format. Fixed the category count — the tables and the `INSERT` both hold **32**; the heading said 30 and the count-check said 31. Dropped "(Seed Set)" from the title and the stray `(proposed — note for the implementer)` marker. |
| 1.0 | 2026-09-07 | Initial. 32 seed categories with naming conventions, a Flyway `INSERT`, and post-launch add/rename/delete notes (incl. the rename→reindex consequence). |
