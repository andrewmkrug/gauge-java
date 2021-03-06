// Copyright 2015 ThoughtWorks, Inc.

// This file is part of Gauge-Java.

// Gauge-Java is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// Gauge-Java is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Gauge-Java.  If not, see <http://www.gnu.org/licenses/>.

package com.thoughtworks.gauge;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import gauge.messages.Api;
import gauge.messages.Spec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GaugeConnection {

    private final int port;
    private Socket gaugeSocket;

    public GaugeConnection(int port) {
        this.port = port;
        createConnection(5);
    }

    public GaugeConnection(Socket socket) {
        gaugeSocket = socket;
        port = socket.getPort();
    }

    private void createConnection(int tries) {
        if (tries == 0) {
            throw new RuntimeException("Gauge API not started");
        }
        try {
            gaugeSocket = new Socket("localhost", port);
        } catch (IOException e) {
            try {
                //waits for the process to start accepting connection
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            createConnection(tries - 1);
        }
    }

    public List<StepValue> fetchAllSteps() throws IOException {
        Api.APIMessage message = getStepRequest();
        Api.APIMessage response = getAPIResponse(message);
        Api.GetAllStepsResponse allStepsResponse = response.getAllStepsResponse();
        List<StepValue> steps = new ArrayList<StepValue>();
        for (Spec.ProtoStepValue stepValueResponse : allStepsResponse.getAllStepsList()) {
            StepValue stepValue = new StepValue(stepValueResponse.getStepValue(), stepValueResponse.getParameterizedStepValue(), stepValueResponse.getParametersList());
            steps.add(stepValue);
        }
        return steps;
    }

    public List<ConceptInfo> fetchAllConcepts() throws IOException {
        Api.APIMessage message = getConceptRequest();
        Api.APIMessage response = getAPIResponse(message);
        Api.GetAllConceptsResponse allConceptsResponse = response.getAllConceptsResponse();
        List<ConceptInfo> conceptsInfo = new ArrayList<ConceptInfo>();
        for (Api.ConceptInfo conceptInfoResponse : allConceptsResponse.getConceptsList()) {
            Spec.ProtoStepValue protoStepValue = conceptInfoResponse.getStepValue();
            StepValue stepValue = new StepValue(protoStepValue.getStepValue(), protoStepValue.getParameterizedStepValue(), protoStepValue.getParametersList());
            ConceptInfo conceptInfo = new ConceptInfo(stepValue,conceptInfoResponse.getFilepath(),conceptInfoResponse.getLineNumber());
            conceptsInfo.add(conceptInfo);
        }
        return conceptsInfo;
    }

    public String getLibPath(String language) throws IOException, PluginNotInstalledException {
        Api.APIMessage message = getLibPathRequest(language);
        Api.APIMessage response = getAPIResponse(message);
        if (response.getMessageType().equals(Api.APIMessage.APIMessageType.ErrorResponse)) {
            throw new PluginNotInstalledException(response.getError().getError());
        }
        return response.getLibPathResponse().getPath();
    }

    private Api.APIMessage getAPIResponse(Api.APIMessage message) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        CodedOutputStream cos = CodedOutputStream.newInstance(stream);
        byte[] bytes = message.toByteArray();
        cos.writeRawVarint64(bytes.length);
        cos.flush();
        stream.write(bytes);
        synchronized (gaugeSocket) {
            gaugeSocket.getOutputStream().write(stream.toByteArray());
            gaugeSocket.getOutputStream().flush();

            InputStream remoteStream = gaugeSocket.getInputStream();
            MessageLength messageLength = getMessageLength(remoteStream);
            bytes = toBytes(messageLength);
        }
        Api.APIMessage apiMessage = Api.APIMessage.parseFrom(bytes);
        return apiMessage;
    }

    public File getInstallationRoot() throws IOException {
        Api.APIMessage installationRootRequest = getInstallationRootRequest();
        Api.APIMessage response = getAPIResponse(installationRootRequest);
        Api.GetInstallationRootResponse allStepsResponse = response.getInstallationRootResponse();
        File installationRoot = new File(allStepsResponse.getInstallationRoot());
        if (installationRoot.exists()) {
            return installationRoot;
        } else {
            throw new IOException(installationRoot + " does not exist");
        }
    }

    public StepValue getStepValue(String stepText) {
        return getStepValue(stepText, false);
    }

    public StepValue getStepValue(String stepText, boolean hasInlineTable) {
        Api.APIMessage stepValueRequest = getStepValueRequest(stepText, hasInlineTable);
        Api.APIMessage response;
        try {
            response = getAPIResponse(stepValueRequest);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Api.GetStepValueResponse stepValueResponse = response.getStepValueResponse();
        Spec.ProtoStepValue protoStepValue = stepValueResponse.getStepValue();
        String stepValue = protoStepValue.getStepValue();
        List<String> parametersList = protoStepValue.getParametersList();
        String parameterizedStepValue = protoStepValue.getParameterizedStepValue();
        return new StepValue(stepValue, parameterizedStepValue, parametersList);
    }


    /**
     * Sends a request to gauge to perform a step name refactoring on the project. 
     * @param oldName old Step Name
     * @param newName New Step Name
     * @return RefactoringResponse message
     * @throws Exception On failure in connecting to gauge core or if invalid message is received.
     */
    public Api.PerformRefactoringResponse sendPerformRefactoringRequest(String oldName, String newName) throws Exception {
        Api.APIMessage refactoringRequest = createPerformRefactoringRequest(oldName, newName);
        Api.APIMessage response = getAPIResponse(refactoringRequest);
        return response.getPerformRefactoringResponse();
    }
    
    public Api.ExtractConceptResponse sendGetExtractConceptRequest(List<Api.step> steps,Api.step concept, boolean changeAcrossProject,String fileName,Api.textInfo selectedTextInfo) throws Exception {
        Api.APIMessage request = createExtractConceptRequest(steps, concept, changeAcrossProject, fileName, selectedTextInfo);
        Api.APIMessage response = getAPIResponse(request);
        return response.getExtractConceptResponse();
    }

    private Api.APIMessage getStepRequest() {
        Api.GetAllStepsRequest stepRequest = Api.GetAllStepsRequest.newBuilder().build();
        return Api.APIMessage.newBuilder()
                .setMessageType(Api.APIMessage.APIMessageType.GetAllStepsRequest)
                .setMessageId(2)
                .setAllStepsRequest(stepRequest)
                .build();
    }

    private Api.APIMessage getInstallationRootRequest() {
        Api.GetInstallationRootRequest installationRootRequest = Api.GetInstallationRootRequest.newBuilder().build();
        return Api.APIMessage.newBuilder()
                .setMessageType(Api.APIMessage.APIMessageType.GetInstallationRootRequest)
                .setMessageId(3)
                .setInstallationRootRequest(installationRootRequest)
                .build();
    }

    private Api.APIMessage getStepValueRequest(String stepText, boolean hasInlineTable) {
        Api.GetStepValueRequest stepValueRequest = Api.GetStepValueRequest.newBuilder().setStepText(stepText).setHasInlineTable(hasInlineTable).build();
        return Api.APIMessage.newBuilder()
                .setMessageType(Api.APIMessage.APIMessageType.GetStepValueRequest)
                .setMessageId(4)
                .setStepValueRequest(stepValueRequest)
                .build();
    }

    private Api.APIMessage getLibPathRequest(String language) {
        Api.GetLanguagePluginLibPathRequest libPathRequest = Api.GetLanguagePluginLibPathRequest.newBuilder().setLanguage(language).build();
        return Api.APIMessage.newBuilder()
                .setMessageType(Api.APIMessage.APIMessageType.GetLanguagePluginLibPathRequest)
                .setMessageId(5)
                .setLibPathRequest(libPathRequest)
                .build();
    }

    private Api.APIMessage getConceptRequest() {
        Api.GetAllConceptsRequest conceptRequest = Api.GetAllConceptsRequest.newBuilder().build();
        return Api.APIMessage.newBuilder()
                .setMessageType(Api.APIMessage.APIMessageType.GetAllConceptsRequest)
                .setMessageId(6)
                .setAllConceptsRequest(conceptRequest)
                .build();
    }

    private Api.APIMessage createPerformRefactoringRequest(String oldName, String newName) {
        Api.PerformRefactoringRequest performRefactoringRequest = Api.PerformRefactoringRequest.newBuilder()
                .setOldStep(oldName)
                .setNewStep(newName)
                .build();
        return Api.APIMessage.newBuilder()
                .setMessageType(Api.APIMessage.APIMessageType.PerformRefactoringRequest)
                .setMessageId(7)
                .setPerformRefactoringRequest(performRefactoringRequest)
                .build();
    }

    private Api.APIMessage createExtractConceptRequest(List<Api.step> steps, Api.step concept, boolean changeAcrossProject, String fileName, Api.textInfo selectedTextInfo) {
        Api.ExtractConceptRequest request = Api.ExtractConceptRequest.newBuilder().addAllSteps(steps).setChangeAcrossProject(changeAcrossProject)
                                            .setSelectedTextInfo(selectedTextInfo).setConceptFileName(fileName).setConceptName(concept).build();
        return Api.APIMessage.newBuilder()
                .setMessageType(Api.APIMessage.APIMessageType.ExtractConceptRequest)
                .setMessageId(8)
                .setExtractConceptRequest(request)
                .build();
    }

    private static MessageLength getMessageLength(InputStream is) throws IOException {
        CodedInputStream codedInputStream = CodedInputStream.newInstance(is);
        long size = codedInputStream.readRawVarint64();
        return new MessageLength(size, codedInputStream);
    }

    private static byte[] toBytes(MessageLength messageLength) throws IOException {
        long messageSize = messageLength.length;
        CodedInputStream stream = messageLength.remainingStream;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < messageSize; i++) {
            outputStream.write(stream.readRawByte());
        }

        return outputStream.toByteArray();
    }
}
