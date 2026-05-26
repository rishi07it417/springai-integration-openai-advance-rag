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
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.print.Doc;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

        return List.of(compression, rewrite);
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

    private List<Document> getDocuments(final InputStream pdfStream) {
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(new InputStreamResource(pdfStream),
                PdfDocumentReaderConfig.builder()
                        .withPagesPerDocument(1)
                        .build());
        return pdfReader.get();
    }

    private List<Document> getCleanedDocuments(final List<Document> documents) {
       return documents.stream().map(doc -> {

            String cleanedText = doc.getText() != null ? doc.getText()
                    .replace("•", "\n")
                    .replace("▪", "\n")
                    .replace("●", "\n ")
                    .replace("○", "\n")
                    .replace("\uF0B7", "\n")
                    : "";
            return new Document(cleanedText, doc.getMetadata());
        }).toList();
    }

    private List<Document> getTransformedDocuments(final List<Document> documents) {
        var textSplitter = TokenTextSplitter.builder().build();

        // Cleaned Documents
        List<Document> cleanDocuments = getCleanedDocuments(documents);

        //Splitted Docs
        List<Document> splitDocs = cleanDocuments.stream()
                .flatMap(doc -> Arrays.stream(doc.getText().split("\n"))
                        .filter(line -> !line.isBlank())
                        .map(line -> new Document(line, doc.getMetadata())))
                .toList();

        return textSplitter.transform(splitDocs);
    }

    public void addDataFromPDF(final InputStream pdfStream) {
        // Document Reader
        List<Document> documents = getDocuments(pdfStream);
        logger.info("BEFORE Transformed PDF document list size: " + documents.size());
        logger.info("BEFORE Transformed PDF document content: " + documents.toString());

        //Document Transformer
        List<Document> transformedDocument = getTransformedDocuments(documents);
        logger.info("AFTER Transformed PDF document list size: " + transformedDocument.size());
        logger.info("AFTER Transformed PDF document content: " + transformedDocument.toString());

        //Add Documents to DB
        this.vectorStore.add(transformedDocument);
    }


}
