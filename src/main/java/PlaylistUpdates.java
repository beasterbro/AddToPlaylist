/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.common.collect.Lists;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Creates a new, private playlist in the authorized user's channel and add
 * a video to that new playlist.
 *
 * @author Jeremy Walker
 */
public class PlaylistUpdates {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static LinkedList<String> video_ids = new LinkedList<>();
    private static YouTube youtubeService ;
    private static final String CLIENT_SECRETS= "client_secret.json";
    private static final Collection<String> SCOPES =
            Arrays.asList("https://www.googleapis.com/auth/youtube");

    private static final String APPLICATION_NAME = "API code samples";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Retrieves all of the video ID's from the inputted file and stores them in an array to be run
     * @param fileName
     */
    public static void getVideoIDS(String fileName){
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
            StringBuilder emptyBuilder = new StringBuilder();
            String currentLine = bufferedReader.readLine();
            while (currentLine != null){
                String[] videoIDS = currentLine.split("\\,", -1);
                video_ids.addAll(Arrays.asList(videoIDS));

               // StringBuilder currVideoID = new StringBuilder();
              /*  for (int i = 0; i< currentLine.length() -1; i++){
                    if (currentLine.charAt(i) == ','){
                        video_ids.add(currVideoID.toString());
                        currVideoID = emptyBuilder ;
                    }
                    else {
                        currVideoID.append(i);
                    }
                }*/

                currentLine = bufferedReader.readLine();
            }
        }catch (IOException  e){
            System.out.println("File Read Error");
        }
    }

    /**
     * Build and return an authorized API client service.
     *
     * @return an authorized API client service
     * @throws GeneralSecurityException, IOException
     */
    public static YouTube getService() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(httpTransport);
        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Create an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize(final NetHttpTransport httpTransport) throws IOException {
        // Load client secrets.
        InputStream in = ApiExample.class.getResourceAsStream(CLIENT_SECRETS);
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                        .build();
        Credential credential =
                new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }


    /**
     * Create a playlist and add it to the authorized account.
     */
    private static String insertPlaylist() throws IOException {

        // This code constructs the playlist resource that is being inserted.
        // It defines the playlist's title, description, and privacy status.
        PlaylistSnippet playlistSnippet = new PlaylistSnippet();
        playlistSnippet.setTitle("Test Playlist " + Calendar.getInstance().getTime());
        playlistSnippet.setDescription("A private playlist created with the YouTube API v3");
        PlaylistStatus playlistStatus = new PlaylistStatus();
        playlistStatus.setPrivacyStatus("private");

        Playlist youTubePlaylist = new Playlist();
        youTubePlaylist.setSnippet(playlistSnippet);
        youTubePlaylist.setStatus(playlistStatus);

        // Call the API to insert the new playlist. In the API call, the first
        // argument identifies the resource parts that the API response should
        // contain, and the second argument is the playlist being inserted.
        YouTube.Playlists.Insert playlistInsertCommand =
                youtubeService.playlists().insert("snippet,status", youTubePlaylist);
        Playlist playlistInserted = playlistInsertCommand.execute();

        // Print data from the API response and return the new playlist's
        // unique playlist ID.
        System.out.println("New Playlist name: " + playlistInserted.getSnippet().getTitle());
        System.out.println(" - Privacy: " + playlistInserted.getStatus().getPrivacyStatus());
        System.out.println(" - Description: " + playlistInserted.getSnippet().getDescription());
        System.out.println(" - Posted: " + playlistInserted.getSnippet().getPublishedAt());
        System.out.println(" - Channel: " + playlistInserted.getSnippet().getChannelId() + "\n");
        return playlistInserted.getId();

    }

    /**
     * Create a playlist item with the specified video ID and add it to the
     * specified playlist.
     *
     * @param playlistId assign to newly created playlistitem
     * @param videoId    YouTube video id to add to playlistitem
     */
    private static String insertPlaylistItem(String playlistId, String videoId) throws IOException {

        // Define a resourceId that identifies the video being added to the
        // playlist.
        ResourceId resourceId = new ResourceId();
        resourceId.setKind("youtube#video");
        resourceId.setVideoId(videoId);

        // Set fields included in the playlistItem resource's "snippet" part.
        PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
        playlistItemSnippet.setTitle("First video in the test playlist");
        playlistItemSnippet.setPlaylistId(playlistId);
        playlistItemSnippet.setResourceId(resourceId);

        // Create the playlistItem resource and set its snippet to the
        // object created above.
        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setSnippet(playlistItemSnippet);

        // Call the API to add the playlist item to the specified playlist.
        // In the API call, the first argument identifies the resource parts
        // that the API response should contain, and the second argument is
        // the playlist item being inserted.
        YouTube.PlaylistItems.Insert playlistItemsInsertCommand =
                youtubeService.playlistItems().insert("snippet,contentDetails", playlistItem);
        PlaylistItem returnedPlaylistItem = playlistItemsInsertCommand.execute();

        // Print data from the API response and return the new playlist
        // item's unique playlistItem ID.

        System.out.println("New PlaylistItem name: " + returnedPlaylistItem.getSnippet().getTitle());
        System.out.println(" - Video id: " + returnedPlaylistItem.getSnippet().getResourceId().getVideoId());
        System.out.println(" - Posted: " + returnedPlaylistItem.getSnippet().getPublishedAt());
        System.out.println(" - Channel: " + returnedPlaylistItem.getSnippet().getChannelId());
        return returnedPlaylistItem.getId();

    }

    /**
     * Authorize the user, create a playlist, and add an item to the playlist.
     *
     * @param args command line args (not used).
     */
    public static void main(String[] args) {

        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account.
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");

        try {

            youtubeService = getService();

            // Create a new, private playlist in the authorized user's channel.
            String playlistId = insertPlaylist();

            // If a valid playlist was created, add a video to that playlist.
            getVideoIDS("links.text");

            for (String s: video_ids) {
                try {
                    insertPlaylistItem(playlistId, s);
                    System.out.println("Added video: " + s);
                }catch (GoogleJsonResponseException e){
                    System.out.println(e + "\nThis video is no longer available: " + s);
                }
            }

        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getMessage());
            t.printStackTrace();
        }
    }

}