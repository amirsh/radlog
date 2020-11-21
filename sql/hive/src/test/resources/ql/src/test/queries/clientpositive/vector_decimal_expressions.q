CREATE TABLE decimal_test STORED AS ORC AS SELECT cdouble, CAST (((cdouble*22.1)/37) AS DECIMAL(20,10)) AS cdecimal1, CAST (((cdouble*9.3)/13) AS DECIMAL(23,14)) AS cdecimal2 FROM alltypesorc;
SET hive.vectorized.execution.enabled=true;
EXPLAIN SELECT cdecimal1 + cdecimal2, cdecimal1 - (2*cdecimal2), ((cdecimal1+2.34)/cdecimal2), (cdecimal1 * (cdecimal2/3.4)), cdecimal1 % 10, CAST(cdecimal1 AS INT), CAST(cdecimal2 AS SMALLINT), CAST(cdecimal2 AS TINYINT), CAST(cdecimal1 AS BIGINT), CAST (cdecimal1 AS BOOLEAN), CAST(cdecimal2 AS DOUBLE), CAST(cdecimal1 AS FLOAT), CAST(cdecimal2 AS STRING), CAST(cdecimal1 AS TIMESTAMP) FROM decimal_test WHERE cdecimal1 > 0 AND cdecimal1 < 12345.5678 AND cdecimal2 != 0 AND cdecimal2 > 1000 AND cdouble IS NOT NULL LIMIT 10;

SELECT cdecimal1 + cdecimal2, cdecimal1 - (2*cdecimal2), ((cdecimal1+2.34)/cdecimal2), (cdecimal1 * (cdecimal2/3.4)), cdecimal1 % 10, CAST(cdecimal1 AS INT), CAST(cdecimal2 AS SMALLINT), CAST(cdecimal2 AS TINYINT), CAST(cdecimal1 AS BIGINT), CAST (cdecimal1 AS BOOLEAN), CAST(cdecimal2 AS DOUBLE), CAST(cdecimal1 AS FLOAT), CAST(cdecimal2 AS STRING), CAST(cdecimal1 AS TIMESTAMP) FROM decimal_test WHERE cdecimal1 > 0 AND cdecimal1 < 12345.5678 AND cdecimal2 != 0 AND cdecimal2 > 1000 AND cdouble IS NOT NULL LIMIT 10;
