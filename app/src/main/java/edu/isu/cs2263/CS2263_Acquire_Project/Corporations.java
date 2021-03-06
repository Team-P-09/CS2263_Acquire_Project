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

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Getter @Setter
public class Corporations {
    private HashMap<String, CorpInfo> corps;

    public Corporations(ArrayList<String> corpNames){
        corps = initializeCorps(corpNames);
    }

    /**
     * Removes all tiles from sub corps and adds them to the dominate corp in the merge
     * @param domCorpName
     * @param subCorpNames
     */
    public void mergeCorps(String domCorpName, List<String> subCorpNames){
        CorpInfo domCorp = getCorp(domCorpName);
        HashMap<String, Tile> domTiles = domCorp.getCorpTiles();

        for(String corpName : subCorpNames){
            CorpInfo subCorp = getCorp(corpName);
            HashMap<String, Tile> subTiles = subCorp.popAllTiles();

            domTiles.putAll(subTiles);
            getCorp(corpName).setStatus(false);
        }
    }

    private HashMap<String, CorpInfo> initializeCorps(ArrayList<String> corpNames){
        HashMap<String, CorpInfo> initCorp = new HashMap<>();

        for(String corpName : corpNames){
            initCorp.put(corpName, new CorpInfo());
        }
        return initCorp;
    }

    public CorpInfo getCorp(String corpName){
        return getCorps().get(corpName);
    }

    /**
     * Calls lower tier method
     * Step 3/4
     * Method Order:
     *      1 - GameState : placeTile
     *      2 - Scoreboard : initCorpTileAdd
     *      3 - Corporations : addTileToCorp
     *      4 - CorpInfo : addCorpTile
     * @param corpName
     * @param t
     */
    public void addTileToCorp(String corpName, Tile t){
        getCorps().get(corpName).addCorpTile(t);
    }

    public String getTilesCorp(Tile t){
        for(String cName : getCorps().keySet()){
            if(getCorp(cName).getCorpTiles().containsKey(t.getLocation())){
                return cName;
            }
        }
        return null; //CHANGE TO THROW EXCEPTION
    }

    /**
     * updates stock tier based on corporation name
     * @param corpName
     */
    public void setStockValue(String corpName){
        Integer corpSize = getCorp(corpName).getCorpSize();
        Integer stockTier; //corps cannot be evaluated at a size less than 2 as no corp can be founded with a size less than 2
        HashMap<Integer, Integer> stockTiers = new HashMap<>();
        for(int i = 1 ; i < 13 ; i++){
            stockTiers.put(i, 100+100*i);
        }

        stockTier = 1 + getCorpBaseTier(corpName) + checkTier(corpSize);

        if(stockTier !=0) {
            getCorp(corpName).setStockPrice(stockTiers.get(stockTier));
        }
    }

    public Integer getBonus(String corpName, String bonusType){
        Integer corpSize = getCorp(corpName).getCorpSize();
        Integer bonusTier;
        Integer bonusAmt = 0;
        HashMap<Integer, Integer> majorityTiers = new HashMap<>();
        HashMap<Integer, Integer> minorityTiers = new HashMap<>();
        for(int i = 0 ; i < 12 ; i++){
            majorityTiers.put(i, 2000+1000*i);
            minorityTiers.put(i, 1000+500*i);
        }

        bonusTier = getCorpBaseTier(corpName) + checkTier(corpSize);

        if(bonusType.equals("Majority")){
            bonusAmt += majorityTiers.get(bonusTier);
        }else{
            bonusAmt += minorityTiers.get(bonusTier);
        }
        return bonusAmt;
    }

    /**
     * Gets corps base tier adjustment
     * @param corpName
     * @return
     */
    private Integer getCorpBaseTier(String corpName){
        Integer bonusTier;
        if(corpName.equals("Imperial") || corpName.equals("Continental")){
            bonusTier = 2;
        }else if(corpName.equals("American") || corpName.equals("Worldwide") || corpName.equals("Festival")){
            bonusTier = 1;
        }else{ //corpName will be Tower or Saxon
            bonusTier = 0;
        }
        return bonusTier;
    }

    /**
     * returns a number to increment the tier of a corporation for accuract retreival or stock price
     * Max return value is 8
     * @param corpSize
     * @return
     */
    private Integer checkTier(Integer corpSize){
        Integer[] tierArray = new Integer[]{2,3,4,5,6,11,21,31,41};
        Integer tierIncrement = 0;
        if(corpSize > 0){
            Integer i = 0;
            while(i<tierArray.length && corpSize >= tierArray[i]){
                tierIncrement = i;
                i++;
            }
        }
        return tierIncrement;
    }
}
