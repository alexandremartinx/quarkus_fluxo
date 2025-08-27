package com.acme.ingest;

import com.acme.common.dto.PriceRecord;
import com.acme.common.events.PriceParsedEvent;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;

@Path("/ingest")
public class IngestResource {

    @Inject ParserService parser;
    @Inject DeltaWriter deltaWriter;
    @Inject MQEmitter emitter;

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(FileUpload file) throws IOException {
        var path = file.uploadedFile();
        int count = 0;
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                PriceRecord rec = parser.parseLine(line);
                if (rec == null) continue;
                deltaWriter.append(rec);
                String id = UUID.nameUUIDFromBytes((path.toString() + "#" + lineNo).getBytes()).toString();
                emitter.emit(new PriceParsedEvent(id, rec));
                count++;
            }
        }
        return Response.ok().entity("{\"ingested\": " + count + "}").build();
    }
}
