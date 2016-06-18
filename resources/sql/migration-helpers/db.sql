--name: -get-constraints
SELECT co.constraintname
FROM sys.sysconstraints co 
JOIN sys.sysschemas sc ON co.schemaid = sc.schemaid 
JOIN sys.systables t ON co.tableid = t.tableid 
JOIN sys.sysforeignkeys f ON co.constraintid = f.constraintid 
JOIN sys.sysconglomerates cg ON f.conglomerateid = cg.conglomerateid 
JOIN sys.sysconstraints co2 ON f.keyconstraintid = co2.constraintid 
JOIN sys.systables t2 ON co2.tableid = t2.tableid 
JOIN sys.syskeys k ON co2.constraintid = k.constraintid 
JOIN sys.sysconglomerates cg2 ON k.conglomerateid = cg2.conglomerateid
WHERE co.type = 'F'
  AND sc.schemaname = current schema
  AND t.tablename = :source_table
  AND t2.tablename = :relation_table

--name: -drop-constraint-from-sighting!
ALTER TABLE sighting
DROP CONSTRAINT :constraint_name
