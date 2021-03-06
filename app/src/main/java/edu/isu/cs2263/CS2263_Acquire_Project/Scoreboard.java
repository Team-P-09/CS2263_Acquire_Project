/*
 * MIT License
 *
 * Copyright (c) 2021 Thomas Evans, David Lindeman, and Natalia Castaneda
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package edu.isu.cs2263.CS2263_Acquire_Project;

import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Getter @Setter
public class Scoreboard {
    private Players players;
    public ArrayList<String> corpNames = new ArrayList<>(Arrays.asList("Worldwide", "Sackson", "Festival", "Imperial", "American", "Tower", "Continental"));
    private Corporations corporations;
    Integer numofPlayers;

    public Scoreboard(Integer numberOfPlayers) {
        corporations = new Corporations(getCorpNames());
        players = new Players(numberOfPlayers, getCorpNames());
        numofPlayers = numberOfPlayers;
    }

    /**
     * Takes corp and player names as well as affected tiles and founds a corporation for the corpName
     * Gives founder an additional tile if corporation had not yet been founded
     * @param tiles
     * @param playerName
     */
    public void initFounding(List<Tile> tiles, String playerName, String corpName){
        String unfoundedCorps = getUnfoundedCorps();

        addUnassignedTilesToCorp(tiles, corpName);
        getCorporations().getCorp(corpName).setStatus(true);

        if(unfoundedCorps.contains(corpName)){
            getPlayers().getPlayerByName(playerName).getPWallet().addStock(corpName, 1);
            getCorporations().getCorps().get(corpName).removeCorpStock(1);
            getCorporations().getCorp(corpName).foundCorp();
        }
        getCorporations().setStockValue(corpName);
    }

    /**
     * Returns all non active corporations
     * @return
     */
    public ArrayList<String> getNonActiveCorps(){
        ArrayList<String> availableCorps = new ArrayList<>();
        for(String cName : getCorpNames()){
            if(!getCorporations().getCorp(cName).isStatus()){
                availableCorps.add(cName);
            }
        }
        return availableCorps;
    }

    /**
     * Returns a list of corps that can be bought from
     * @return
     */
    public ArrayList<String> getBuyableCorps(){
        ArrayList<String> availableCorps = new ArrayList<>();
        for(String cName : getCorpNames()){
            if(getCorporations().getCorp(cName).isStatus()){
                availableCorps.add(cName);
            }
        }
        return availableCorps;
    }

    /**
     * Returns a list of corps that have not been founded
     * @return
     */
    private String getUnfoundedCorps(){
        String unfoundedCorps = "";
        String newCorp;
        for(String cName : getCorpNames()){
            if(!getCorporations().getCorp(cName).isHasBeenFounded()){
                newCorp = cName + "\n";
                unfoundedCorps += newCorp;
            }
        }
        return unfoundedCorps;
    }

    public void initMerge(List<String> mCorps, String domCorpName, List<String> affectedPlayers){
        for(String mcorp : mCorps){
            HashMap<String, List<String>> stockHolderRankings = getStockHolderRankings(affectedPlayers, mcorp);
            divyOutBonuses(stockHolderRankings,mcorp,"Majority");
            divyOutBonuses(stockHolderRankings,mcorp,"Minority");
        }

        //Runs the merge function changing the number of tiles
        getCorporations().mergeCorps(domCorpName, mCorps);

        //Update stock prices
        for(String subCName : mCorps){
            getCorporations().setStockValue(subCName);
        }
        getCorporations().setStockValue(domCorpName);
    }

    public void addUnassignedTilesToCorp(List<Tile> tList, String corpName){
        for(Tile t : tList){
            if(t.isStatus() && t.getCorp() == null){
                getCorporations().addTileToCorp(corpName, t);
            }
        }
    }

    /**
     * Returns the name of the corporation with the greatest size
     * If there is a tie it prompts the user for input
     * @param mCorps
     * @return
     */
    public String getDomCorpName(List<String> mCorps){
        List<String> domCorp = findDomCorp(mCorps);
        String domCorpName;

        if(checkMergeStatus(domCorp)){
            domCorpName = getDecision(domCorp, "Corporations tied for merger", "Chose the dominate corporation");//domCorp.get(choiceIndex);
        }else{
            domCorpName = domCorp.get(0);
        }
        return domCorpName;
    }

    /**
     * Runs merge tern for affected players
     * Calls mergeTurn which utilizes the ChoiceDialogue box
     * @param mCorps
     * @param domCorpName
     * @param affectedPlayers
     */
    public void runMergeTurn(List<String> mCorps, String domCorpName, List<String> affectedPlayers) {
        for(String player : affectedPlayers){
            mergeTurn(player, domCorpName, mCorps);
        }
    }

    public List<Tile> retrieveTiles(HashMap<String, Tile> corpTiles){
        List<Tile> outTiles = new ArrayList<>();
        for(String tileLoc : corpTiles.keySet()){
            outTiles.add(corpTiles.get(tileLoc));
        }
        return outTiles;
    }


    /**
     * gives bonuses to players based on the number of stocks they own
     * @param bonusList
     * @param corpName
     * @param bonusType
     */
    private void divyOutBonuses(HashMap<String, List<String>> bonusList, String corpName, String bonusType){
        List<String> players = bonusList.get(bonusType);
        Integer bonusAmt = 0;
        for(String playerName : players){
            bonusAmt = getCorporations().getBonus(corpName, bonusType);
            getPlayers().getPlayerByName(playerName).getPWallet().addCash(bonusAmt/players.size());
        }
    }

    /**
     * Returns a hashmap of stock holders and their rankings
     * @param players
     * @param corpName
     * @return
     */
    private HashMap<String, List<String>> getStockHolderRankings(List<String> players, String corpName){
        Integer playerScore;
        HashMap<String, Integer> scoreResults = new HashMap<>();
        HashMap<String, List<String>> playerPlaces = new HashMap<>();
        List<String> majPlayers = new ArrayList<>();
        List<String> minPlayers = new ArrayList<>();

        for(String player : players){
            playerScore = getPlayers().getPlayerByName(player).getPWallet().getStocks().get(corpName); //getPlayerScore(playerName);
            scoreResults.put(player, playerScore);
        }

        HashMap<String, Integer> sortedPlayers = sortHashmapByValuesInDescendingOrder(scoreResults);

        Integer lastValue = 0;
        Integer pScore;
        Boolean majorityBool = true;
        Boolean minorityBool = true;

        for(String pName : sortedPlayers.keySet()){
            pScore = scoreResults.get(pName);

            //If the current score does not equal the last score then it must be less
            //if the last value is 0 then this is the first iteration and the player will be in the majority
            //if the values are not the same then the next true boolean (majority or minority) will be turned to false
            //if both majority and minority booleans are false then it breaks as we dont divy bonuses to those players
            if(pScore != lastValue && lastValue != 0){
                if(majorityBool){
                    majorityBool = false;
                }else if(minorityBool){
                    minorityBool = false;
                }
            }
            if(majorityBool){
                majPlayers.add(pName);
            }else if(minorityBool){
                minPlayers.add(pName);
            }else{break;}

            lastValue = pScore;
        }
        playerPlaces.put("Majority", majPlayers);
        playerPlaces.put("Minority", minPlayers);


        return playerPlaces;
    }


    /**
     * removes all corp names for corporations that have sizes equal to or over 11
     * @param mCorps
     * @return ArrayList of Strings containing the names of unsafe corporations
     */
    public List<String> removeSafeCorps(List<String> mCorps){
        List<String> unsafeCorps = new ArrayList<>();
        for(String corpName : mCorps){
            if(!getCorporations().getCorp(corpName).isSafe()){
                unsafeCorps.add(corpName);
            }
        }
        return unsafeCorps;
    }

    /**
     * Returns a list of player names for all players who have 1 or more stocks in the affected corporation
     * @param mCorps
     * @return
     */
    public List<String> findAffectedPlayers(List<String> mCorps){
        List<String> affectedPlayers = new ArrayList<>();
        for(String cName : mCorps){
            for(PlayerInfo player : getPlayers().getActivePlayers()){
                if(player.getPWallet().getStocks().containsKey(cName)){
                    if(player.getPWallet().getStocks().get(cName) > 0){
                        affectedPlayers.add(player.getPName());
                    }
                }
            }
        }
        return affectedPlayers;
    }

    private void mergeTurn(String playerName, String domCorpName, List<String> subCorps){
        PlayerInfo affectedPlayer = getPlayers().getPlayerByName(playerName);
        List<String> choiceList = new ArrayList<>(Arrays.asList("Sell", "Trade", "Hold"));
        String choiceTitle  = "Merge Turn for " + playerName;
        String choiceHeader;
        String decision;
        for(String subCorpName : subCorps){
            choiceHeader = "For corporation " + subCorpName + " choose an action, when you are finished press Hold";
            decision = getDecision(choiceList, choiceTitle, choiceHeader);
            while(decision != choiceList.get(2)){
                if(decision.equals(choiceList.get(0))){
                    initSell(playerName, subCorpName,true);
                }else if(decision.equals(choiceList.get(1))){
                    mergeTrade(playerName, subCorpName, domCorpName);
                }else if(affectedPlayer.getPWallet().getStocks().get(subCorpName) == 0){ //happens after a player doesnt have any more stocks for the corporation
                    break; //removes the need for the player to click "Next Corp" when they have no more stocks
                }//hold is the absence of action
                decision = getDecision(choiceList, choiceTitle, choiceHeader);
            }
        }
    }

    private void mergeTrade(String playerName, String subCorpName, String domCorpName){
        Integer maxGetQty = maxMerge(playerName, subCorpName, domCorpName);
        Integer domQty = getQty(subCorpName, maxGetQty, "Trade");
        Integer adjSubQty = maxGetQty * 2;

        getPlayers().getPlayerByName(playerName).getPWallet().removeStock(subCorpName, adjSubQty);
        getCorporations().getCorp(subCorpName).addCorpStock(adjSubQty);

        getPlayers().getPlayerByName(playerName).getPWallet().addStock(domCorpName, domQty);
        getCorporations().getCorp(domCorpName).removeCorpStock(domQty);
    }

    private Integer maxMerge(String playerName, String subCorpName, String domCorpName){
        Integer maxSubSell = maxSell(playerName, subCorpName);
        Integer tradeQty = maxSubSell/2;
        Integer availableStock = getCorporations().getCorp(domCorpName).getAvailableStocks();
        if(tradeQty > availableStock){
            return availableStock;
        }else{
            return tradeQty;
        }
    }

    public Integer getQty(String corpName, Integer maxVal, String operation){
        Integer qty = 0;
        boolean canBeInt = false;
        if(maxVal > 0){
            Integer newQty;
            TextInputDialog dialog = new TextInputDialog("0");
            dialog.setTitle(operation + " operation for " + corpName);
            dialog.setHeaderText("Maximum " + operation + " value of " + String.valueOf(maxVal));
            dialog.setContentText("Please enter a value:");
            while(!canBeInt){
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()){
                    try{
                        newQty = Integer.parseInt(result.get());
                        if(newQty <= maxVal && newQty >= 0){
                            qty = newQty;
                            canBeInt = true;
                        }
                    }catch(Exception e){
                        canBeInt = false;
                    }
                }
            }
        }
        return qty;
    }

    public <T> T getDecision(List<T> choiceList, String title, String header){
        ArrayList<T> choices = new ArrayList<>();
        for(T choice : choiceList){
            choices.add(choice);
        }

        ChoiceDialog<T> dialog = new ChoiceDialog<>(choiceList.get(0), choices);
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        Optional<T> pChoice = dialog.showAndWait();
        while(!pChoice.isPresent()){
            pChoice = dialog.showAndWait();
        }
        return dialog.getSelectedItem();
    }

    /**
     * Initiates and controls the sell operation updating the available stocks for a corp and updating the sock and cash of a player
     * @param playerName
     * @param corpName
     */
    public void initSell(String playerName, String corpName, Boolean isMerge){
        Integer maxStock = maxSell(playerName, corpName);
        Integer qty = getQty(corpName, maxStock, "Sell");
        int stockVal = getCorporations().getCorp(corpName).getStockPrice();
        if(isMerge){stockVal = stockVal/2;}
        getPlayers().sellStock(playerName, corpName, qty, stockVal);
        getCorporations().getCorp(corpName).addCorpStock(qty);
    }

    /**
     * returns maximum value the player can sell
     * @param playerName
     * @param corpName
     * @return
     */
    private Integer maxSell(String  playerName, String corpName){
        return getPlayers().getPlayerByName(playerName).getPWallet().getStocks().get(corpName);
    }

    /**
     * Removes available stock from specific CorpInfo
     * Removes cash form specific Player
     * Adds stock value to specific player
     * @param playerName
     * @param corpName
     */
    public Integer initBuy(String playerName, String corpName, Integer qty){
        int stockVal = getCorporations().getCorp(corpName).getStockPrice();
        getPlayers().buyStock(playerName,corpName, qty, stockVal);
        getCorporations().getCorp(corpName).removeCorpStock(qty);
        return qty;
    }

    /**
     * Verifies players wallet to find the maximum number of stock to buy
     * @param playerName
     * @param corpName
     * @return
     */
    public Integer maxBuy(String playerName, String corpName, Integer buyLimit){
        Integer stockPrice = getCorporations().getCorp(corpName).getStockPrice();
        Integer availableStock = getCorporations().getCorp(corpName).getAvailableStocks();
        Integer pCash = getPlayers().getPlayerByName(playerName).getPWallet().getCash();
        Integer buyableStock;
        if(pCash/stockPrice > availableStock){
            buyableStock = availableStock;
        }else{
            buyableStock = pCash/stockPrice;
        }
        if(buyableStock > buyLimit){
            return buyLimit;
        }else{
            return buyableStock;
        }
    }

    /**
     * Returns true if there are two or more corps tied for largest size
     * Returns false if there is only one dominate corp
     * @param domList
     * @return
     */
    private boolean checkMergeStatus(List<String> domList){
        if(domList.size() > 1){
            return true;
        }
        return false;
    }

    /**
     * Returns a corp name as a string
     * used in initMerge
     * iterates through the Array of Tiles returning the name
     * @param tArray
     * @return
     */
    public ArrayList<String> findCorps(List<Tile> tArray){
        ArrayList<String> cNames = new ArrayList<>();
        String cName;
        for(Tile t : tArray){
            cName = getCorporations().getTilesCorp(t);
            if(cName != null){
                if(!cNames.contains(cName)){
                    cNames.add(cName);
                }
            }
        }
        return cNames;
    }

    /**
     * Returns an ArrayList of Strings
     * Usually will be a single entry
     * @param mCorps
     * @return      Returns the dominate corp name(s)
     */
    private List<String> findDomCorp(List<String> mCorps){
        int leadingCorpSize = 0;
        List<String> domCorpList = new ArrayList<>();
        int cSize;
        for(String s : mCorps){
            cSize = getCorporations().getCorp(s).getCorpSize();
            if(leadingCorpSize < cSize){
                domCorpList.clear();
                domCorpList.add(s);
                leadingCorpSize = cSize;

            } else if(leadingCorpSize == cSize){
                domCorpList.add(s);
            }
        }
        return domCorpList;
    }

    public HashMap<String, Integer> getWinners(){
        Integer playerScore;
        String playerName;
        HashMap<String, Integer> scoreResults = new HashMap<>();
        HashMap<String, Integer> playerPlaces = new HashMap<>();

        for(PlayerInfo player : getPlayers().getActivePlayers()){
            playerName = player.getPName();
            playerScore = getPlayerScore(playerName);
            scoreResults.put(playerName, playerScore);
        }

        HashMap<String, Integer> sortedScores = sortHashmapByValuesInDescendingOrder(scoreResults);

        Integer currentPlace = 1;
        Integer newPlace = currentPlace;
        Integer lastValue = 0;
        Integer pScore;
        for(String pName : sortedScores.keySet()){
            pScore = scoreResults.get(pName);
            if(lastValue == 0 || pScore != lastValue){
                currentPlace = newPlace;
            }
            playerPlaces.put(pName, currentPlace);

            newPlace++;
            lastValue = pScore;
        }

        return playerPlaces;
    }

    /**
     * Return a sorted (descending) LinkedHashMap of any hashmap with values of Integers
     * @param scoreResults
     * @return
     */
    private HashMap<String, Integer> sortHashmapByValuesInDescendingOrder(Map<String, Integer> scoreResults){
        //stream has a sorted method that we will utilize
        Stream<Map.Entry<String, Integer>> scoreResultsStream = scoreResults.entrySet().stream();
        //we utilize the sorted method of a stream to iterate through the map then use comparator to compare the values
        //sort naturally returns in ascending order, negative value used to invert the order into decending
        Stream<Map.Entry<String, Integer>> scoreResultsSteamSorted = scoreResultsStream.sorted(Comparator.comparingInt(p -> -p.getValue()));
        HashMap<String, Integer> sortedPlayers = scoreResultsSteamSorted.collect(Collectors.toMap(
                //using :: get each key and value from the stream that we will have sorted by value (integer)
                Map.Entry::getKey,
                Map.Entry::getValue,
                //if we run into any errors assigning our player/scoreboard pairs we'll throw an assertion error
                (k, v) -> {throw new AssertionError();},
                //Creating a new Linked hashmap that will have all of our players and their number of stocks sorted by stock value decending
                LinkedHashMap::new
        ));
        return sortedPlayers;
    }

    /**
     * Sorts through the array and returns the corp name and the tile to be added
     * Step 2/4
     * Method Order:
     *      1 - GameState : placeTile
     *      2 - Scoreboard : initCorpTileAdd
     *      3 - Corporations : addTileToCorp
     *      4 - CorpInfo : addCorpTile
     */
    public List<Tile> initCorpTileAdd(List<Tile> tList){ //String corpName, Tile t
        String corpName = "";
        List<Tile> tilesToAdd = new ArrayList<>();
        for(Tile t : tList){
            if(t.isStatus() && t.getCorp() != null){
                corpName = t.getCorp();
            }else if(t.isStatus()){tilesToAdd.add(t);}
        }
        for(Tile t : tilesToAdd){
            getCorporations().addTileToCorp(corpName, t);
        }
        getCorporations().setStockValue(corpName);
        return tilesToAdd;
    }


    public Integer getPlayerScore(String playerName) {
        Integer pScore = getPlayers().getPlayerByName(playerName).getPWallet().getCash();
        HashMap<String, Integer> pStocks = getPlayers().getPlayerByName(playerName).getPWallet().getStocks();
        Integer stockPrice;
        Integer stockQty;
        for (String stockCorp : pStocks.keySet()) {
            stockPrice = getCorporations().getCorp(stockCorp).getStockPrice();
            stockQty = getPlayers().getPlayerByName(playerName).getPWallet().getStocks().get(stockCorp);
            pScore += stockQty * stockPrice;
        }
        return pScore;
    }


    /**
     * @param jsonFile (string to become json file)
     * @param scoreboard_obj (scoreboard obj to save)
     * @return File (jsonFile to later be deserialized)
     * @throws IOException
     */
    //reference for reading JSON files to java: https://attacomsian.com/blog/gson-read-json-file
    public static File saveScoreboard(String jsonFile, Scoreboard scoreboard_obj) throws IOException {
        //create Gson instance
        Gson gson = new Gson();
        //create json string to hold data
        String jsonString = gson.toJson(scoreboard_obj);

        try {
            //create the jsonFile
            File sboardFile = new File(jsonFile);

            //write the json string into the json file
            FileWriter fileWriter = new FileWriter(sboardFile);
            fileWriter.write(jsonString);

            //close the file
            fileWriter.flush();
            fileWriter.close();

            return sboardFile;

        } catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param jsonFile (jsonFile string that was created in saveScoreboard)
     * @return returns a scoreboard object that was previously saved
     */
    public static Scoreboard loadScoreboard(String jsonFile) {
        try {
            //create Gson instance
            Gson gson = new Gson();

            //create a reader
            Reader reader = Files.newBufferedReader(Paths.get(jsonFile));

            //set type for scoreboard
            Type scoreboardType = new TypeToken<Scoreboard>(){}.getType();

            //convert JSON string to scoreboard obj
            Scoreboard scoreboard_obj = gson.fromJson(reader, scoreboardType);

            //close reader
            reader.close();

            return scoreboard_obj;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
