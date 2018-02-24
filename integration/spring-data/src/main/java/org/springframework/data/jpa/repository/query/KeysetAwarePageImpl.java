/*
 * Copyright 2014 - 2018 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.jpa.repository.query;

import com.blazebit.persistence.KeysetPage;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.spring.data.api.repository.KeysetAwarePage;
import com.blazebit.persistence.spring.data.api.repository.KeysetPageRequest;
import com.blazebit.persistence.spring.data.api.repository.KeysetPageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * @author Christian Beikov
 * @since 1.2.0
 */
public class KeysetAwarePageImpl<T> extends PageImpl<T> implements KeysetAwarePage<T> {

    private final KeysetPage keysetPage;

    public KeysetAwarePageImpl(List<T> list) {
        super(list);
        this.keysetPage = null;
    }

    public KeysetAwarePageImpl(PagedList<T> list, Pageable pageable) {
        super(list, keysetPageable(list.getKeysetPage(), pageable), list.getTotalSize());
        this.keysetPage = list.getKeysetPage();
    }

    public KeysetAwarePageImpl(List<T> list, long totalSize, KeysetPage keysetPage, Pageable pageable) {
        super(list, keysetPageable(keysetPage, pageable), totalSize);
        this.keysetPage = keysetPage;
    }

    @Override
    public KeysetPage getKeysetPage() {
        return keysetPage;
    }

    @Override
    public KeysetPageable nextPageable() {
        return (KeysetPageable) super.nextPageable();
    }

    @Override
    public KeysetPageable previousPageable() {
        return (KeysetPageable) super.previousPageable();
    }

    private static Pageable keysetPageable(KeysetPage keysetPage, Pageable pageable) {
        if (pageable instanceof KeysetPageRequest) {
            return pageable;
        }

        if (keysetPage == null) {
            return new KeysetPageRequest(null, pageable.getSort(), pageable.getPageNumber(), pageable.getPageSize());
        } else {
            return new KeysetPageRequest(keysetPage, pageable.getSort());
        }
    }
}
