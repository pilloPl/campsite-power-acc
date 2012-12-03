-- Whole forest with the path of names from the root and the depth (root = 1).
CREATE VIEW V_GROUP (ID, PARENT_ID, NAME, TYPENAME, NAME_PATH, LVL) AS
WITH RECURSIVE tree (id, parent_id, name, typename, name_path, lvl) AS (
    SELECT g.ID, g.PARENT_ID, g.NAME, g.TYPENAME, CAST(' | ' || g.NAME AS VARCHAR(4000)), 1
      FROM TBL_GROUP g
     WHERE g.PARENT_ID IS NULL
    UNION ALL
    SELECT c.ID, c.PARENT_ID, c.NAME, c.TYPENAME, CAST(t.name_path || ' | ' || c.NAME AS VARCHAR(4000)), t.lvl + 1
      FROM TBL_GROUP c
      JOIN tree t ON c.PARENT_ID = t.id
)
SELECT id, parent_id, name, typename, name_path, lvl FROM tree;
