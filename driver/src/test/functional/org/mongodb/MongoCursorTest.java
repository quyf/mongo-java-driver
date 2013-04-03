/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MongoCursorTest extends DatabaseTestCase {

    private MongoCursor<Document> cursor;

    @Before
    public void setUp() {
        super.setUp();
        for (int i = 0; i < 10; i++) {
            collection.insert(new Document("_id", i));
        }
    }

    @After
    public void tearDown() {
        super.tearDown();
        if (cursor != null) {
            cursor.close();
        }
    }

    @Test
    public void testLimitWithGetMore() {
        final List<Document> list = new ArrayList<Document>();
        collection.batchSize(2).limit(5).into(list);
        assertEquals(5, list.size());
    }

    @Test
    public void testLimitWithLargeDocuments() {
        char[] array = new char[16000];
        Arrays.fill(array, 'x');
        final String bigString = new String(array);

        for (int i = 11; i < 1000; i++) {
            collection.insert(new Document("_id", i).append("s", bigString));
        }

        final List<Document> list = new ArrayList<Document>();
        collection.limit(300).into(list);
        assertEquals(300, list.size());
        for (Document document : list) {
            System.out.println(document.get("_id"));
        }
    }

    @Test
    public void testNormalLoopWithGetMore() {
        cursor = collection.sort(new Document("_id", 1)).batchSize(2).all();
        try {
            int i = 0;
            while (cursor.hasNext()) {
                final Document cur = cursor.next();
                assertEquals(i++, cur.get("_id"));
            }
            assertEquals(10, i);
            assertFalse(cursor.hasNext());
        } finally {
            cursor.close();
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void testNextWithoutHasNextWithGetMore() {
        cursor = collection.sort(new Document("_id", 1)).batchSize(2).all();
        for (int i = 0; i < 10; i++) {
            final Document cur = cursor.next();
            assertEquals(i, cur.get("_id"));
        }
        assertFalse(cursor.hasNext());
        assertFalse(cursor.hasNext());
        cursor.next();
    }

    @Test
    public void testLimit() {
        final List<Document> list = new ArrayList<Document>();
        collection.limit(4).into(list);
        assertEquals(4, list.size());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotBeAbleToCallNextAfterCursorIsClosed() {
        cursor = collection.all();

        cursor.next();
        cursor.close();
        cursor.next();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotBeAbleToCallHasNextAfterClose() {
        cursor = collection.all();

        cursor.next();
        cursor.close();
            cursor.hasNext();
    }
}
