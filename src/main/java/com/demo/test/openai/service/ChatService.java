package com.demo.test.openai.service;

import com.demo.test.openai.model.AppChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.print.Doc;
import java.util.List;
import java.util.Map;

import static com.demo.test.openai.contants.AppContants.*;

@Service
public class ChatService {

    private final ChatClient customChatClient;
    private final Resource systemPrompt;
    private final VectorStore vectorStore;

    private Logger logger = LoggerFactory.getLogger(ChatService.class);

    ChatService(@Qualifier("customChatClient") ChatClient customChatClient,
                @Value("classpath:prompts/AppSystemPrompt.st") Resource systemPrompt,
                VectorStore vectorStore) {
        this.customChatClient = customChatClient;
        this.systemPrompt = systemPrompt;
        this.vectorStore = vectorStore;
    }

    private List<QueryTransformer> getPreRetrivalAugmentationAdvisor() {
        // PRE Retrieval Query Transformation with multiple transformers

        // Compersion Query Transformer
        CompressionQueryTransformer compression = CompressionQueryTransformer.builder()
                .chatClientBuilder(customChatClient.mutate().clone())
                .build();

        // Rewrite Query Transformer
        RewriteQueryTransformer rewrite = RewriteQueryTransformer.builder()
                .chatClientBuilder(customChatClient.mutate().clone())
                .build();

        // Translation Query Transformer
        TranslationQueryTransformer translation = TranslationQueryTransformer.builder()
                .chatClientBuilder(customChatClient.mutate().clone())
                .targetLanguage("en")
                .build();

        return List.of(compression, rewrite, translation);
    }

    private DocumentRetriever getDocumentRetriever() {
        // RETRIVAL Augmentation Advisor with multiple query transformers
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(this.vectorStore)
                .topK(2)
                .similarityThreshold(0.8)
                .build();

        return documentRetriever;
    }

    private RetrievalAugmentationAdvisor getRetrievalAugmentationAdvisor() {
        return  RetrievalAugmentationAdvisor.builder()
                .queryTransformers(getPreRetrivalAugmentationAdvisor())
                .documentRetriever(getDocumentRetriever())
                .documentJoiner(new ConcatenationDocumentJoiner())
                .build();
    }


    public AppChatResponse chatWithFluentApi(final String userId, final String message) {

        // Call the ChatClient with the combined prompt
        AppChatResponse content = this.customChatClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec
                        .advisors(getRetrievalAugmentationAdvisor())
                        .params(Map.of("useId", userId)))
                .system(promptSystemSpec ->
                        promptSystemSpec.text(this.systemPrompt))
                .call().entity(AppChatResponse.class);
        logger.info("Fluent API Chat response: " + content.toString());
        return content;
    }

    public void addData(final List<String> dataList) {
        List<Document> documents = dataList.stream().map(data -> Document.builder().text(data).build()).toList();
        this.vectorStore.add(documents);
    }


}
