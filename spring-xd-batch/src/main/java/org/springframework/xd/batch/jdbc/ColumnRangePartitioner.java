/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.batch.jdbc;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * A simple partitioner for a range of values of a column in a database
 * table. Works best if the values are uniformly distributed (e.g.
 * auto-generated primary key values).
 *
 * @author Dave Syer
 * @author Thomas Risberg
 */
public class ColumnRangePartitioner implements Partitioner {

	private JdbcOperations jdbcTemplate;

	private String table;

	private String column;

	private int partitions;

	/**
	 * The data source for connecting to the database.
	 *
	 * @param dataSource a {@link DataSource}
	 */
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * The name of the SQL table the data are in.
	 *
	 * @param table the name of the table
	 */
	public void setTable(String table) {
		this.table = table;
	}

	/**
	 * The name of the column to partition.
	 *
	 * @param column the column name.
	 */
	public void setColumn(String column) {
		this.column = column;
	}

	/**
	 * The number of partitions to create.
	 *
	 * @param partitions the number of partitions.
	 */
	public void setPartitions(int partitions) {
		this.partitions = partitions;
	}

	/**
	 * Partition a database table assuming that the data in the column specified
	 * are uniformly distributed. The execution context values will have keys
	 * <code>minValue</code> and <code>maxValue</code> specifying the range of
	 * values to consider in each partition.
	 *
	 * @see Partitioner#partition(int)
	 */
	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {

		Map<String, ExecutionContext> result = new HashMap<String, ExecutionContext>();
		if (StringUtils.hasText(column) && StringUtils.hasText(table)) {
			int min = jdbcTemplate.queryForObject("SELECT MIN(" + column + ") from " + table, Integer.class);
			int max = jdbcTemplate.queryForObject("SELECT MAX(" + column + ") from " + table, Integer.class);

			int targetSize = (max - min) / partitions + 1;

			int number = 0;
			int start = min;
			int end = start + targetSize - 1;

			while (start <= max) {
				ExecutionContext value = new ExecutionContext();
				result.put("partition" + number, value);

				if (end >= max) {
					end = max;
				}
				value.putString("partClause", "WHERE " + column + " BETWEEN " + start + " AND " + end);
				value.putString("partSuffix", "-p"+number);
				start += targetSize;
				end += targetSize;
				number++;
			}
		}
		else {
			ExecutionContext value = new ExecutionContext();
			result.put("partition" + 0, value);
			value.putString("partClause", "");
			value.putString("partSuffix", "");
		}

		return result;
	}
}
