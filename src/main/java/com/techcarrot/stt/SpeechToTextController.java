package com.techcarrot.stt;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;
import com.techcarrot.domain.Message;

import ws.schild.jave.AudioAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncodingAttributes;
import ws.schild.jave.MultimediaObject;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/speech")
public class SpeechToTextController {

	@ResponseBody
	@GetMapping("/transcribe")
	public ResponseEntity<Message> transcribeAudio() throws Exception {

		/*
		 * CredentialsProvider credentialsProvider;
		 * 
		 * SpeechSettings settings = null;
		 * 
		 * @Autowired public void setCredentialsProvider(CredentialsProvider
		 * credentialsProvider) { this.credentialsProvider = credentialsProvider; }
		 */

		// Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
		try (SpeechClient speech = SpeechClient.create()) {

			// Setting audio configurations
			RecognitionConfig config = RecognitionConfig.newBuilder().setEncoding(AudioEncoding.FLAC)
					.setLanguageCode("en-US").setEnableAutomaticPunctuation(true).setEnableWordTimeOffsets(true)
					.setModel("default").build();

			// Local audio file Set
			Path path = Paths.get("C:\\Users\\sayan\\Documents\\audio-file.flac");
			byte[] data = Files.readAllBytes(path);
			ByteString audioBytes = ByteString.copyFrom(data);
			RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

			// Cloud audio file set
			// RecognitionAudio audio =
			// RecognitionAudio.newBuilder().setUri("gs://sayanti/audio.flac").build();

			// Use non-blocking call for getting file transcription
			OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response = speech
					.longRunningRecognizeAsync(config, audio);

			while (!response.isDone()) {
				System.out.println("Waiting for response...");
				Thread.sleep(10000);
			}

			List<SpeechRecognitionResult> speechResults = response.get().getResultsList();

			StringBuilder transcription = new StringBuilder();
			for (SpeechRecognitionResult speechResult : speechResults) {
				SpeechRecognitionAlternative alternative = speechResult.getAlternativesList().get(0);
				transcription.append(alternative.getTranscript());
			}
			Message message = new Message();
			message.setTranscript(transcription.toString());
			// System.out.printf("Transcription: %s%n", transcription.toString());
			return new ResponseEntity<Message>(message, HttpStatus.OK);

		}

	}

	@ResponseBody
	@PostMapping(value = "/search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Message> searchByAudio(@RequestBody MultipartFile sourceFile) throws Exception {

		System.out.print("Inside Search");
		System.out.print("Got the file here:" + sourceFile);
		
		//Conversion to flac from webm format
		File targetFile = new File("output.flac");
		FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
		byte[] bytes = sourceFile.getInputStream().readAllBytes();
		fileOutputStream.write(bytes);
		fileOutputStream.flush();
		fileOutputStream.close();
		
		AudioAttributes audioAttr = new AudioAttributes();
		audioAttr.setCodec("flac");
		audioAttr.setBitRate(64000);
		audioAttr.setChannels(2);
		audioAttr.setSamplingRate(44100);
		
		EncodingAttributes attrs = new EncodingAttributes();
		attrs.setFormat("flac");
		attrs.setAudioAttributes(audioAttr);
		
		try {
			  Encoder encoder = new Encoder();  
			  encoder.encode((MultimediaObject) sourceFile, targetFile, attrs);
			} catch (Exception e) {  
			   /*Handle here the video failure*/   
			   e.printStackTrace();
			}
		

		// Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
		try (SpeechClient speech = SpeechClient.create()) {

			// Setting audio configurations
			RecognitionConfig config = RecognitionConfig.newBuilder().setEncoding(AudioEncoding.FLAC)
					.setSampleRateHertz(16000).setLanguageCode("en-US").setEnableAutomaticPunctuation(true)
					.setEnableWordTimeOffsets(true).setModel("default").build();

			// Local audio file Set
			// Path path = Paths.get("C:\\Users\\sayan\\Documents\\audio-file.flac");
			byte[] data =  Files.readAllBytes(targetFile.toPath());
			ByteString audioBytes = ByteString.copyFrom(data);
			RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

			// Use non-blocking call for getting file transcription
			OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response = speech
					.longRunningRecognizeAsync(config, audio);

			while (!response.isDone()) {
				System.out.println("Waiting for response...");
				Thread.sleep(10000);
			}

			List<SpeechRecognitionResult> speechResults = response.get().getResultsList();

			StringBuilder transcription = new StringBuilder();
			for (SpeechRecognitionResult speechResult : speechResults) {
				SpeechRecognitionAlternative alternative = speechResult.getAlternativesList().get(0);
				transcription.append(alternative.getTranscript());
			}
			Message message = new Message();
			message.setTranscript(transcription.toString());
			// System.out.printf("Transcription: %s%n", transcription.toString());
			return new ResponseEntity<Message>(message, HttpStatus.OK);

		}
	}

}
