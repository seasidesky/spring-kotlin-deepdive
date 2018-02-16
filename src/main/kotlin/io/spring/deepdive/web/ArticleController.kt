/*
 * Copyright 2002-2017 the original author or authors.
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
package io.spring.deepdive.web

import io.spring.deepdive.MarkdownConverter
import io.spring.deepdive.model.Article
import io.spring.deepdive.repository.ArticleEventRepository
import io.spring.deepdive.repository.ArticleRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/article")
class ArticleController(private val articleRepository: ArticleRepository,
                        private val articleEventRepository: ArticleEventRepository,
                        private val markdownConverter: MarkdownConverter) {

    val notifications = articleEventRepository.count().flatMapMany { articleEventRepository.findWithTailableCursorBy().skip(it) }.share()

    @GetMapping("/")
    fun findAll() = articleRepository.findAllByOrderByAddedAtDesc()

    @GetMapping("/{slug}")
    fun findOne(@PathVariable slug: String, @RequestParam converter: String?) = when (converter) {
        "markdown" -> articleRepository.findById(slug).map { it.copy(
                headline = markdownConverter.invoke(it.headline),
                content = markdownConverter.invoke(it.content)) }
        null -> articleRepository.findById(slug)
        else -> throw IllegalArgumentException("Only markdown converter is supported")
    }

    @PostMapping("/")
    fun save(@RequestBody article: Article) = articleRepository.save(article)

    @DeleteMapping("/{slug}")
    fun delete(@PathVariable slug: String) = articleRepository.deleteById(slug)

    @GetMapping("/notifications", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun notifications() = notifications

}