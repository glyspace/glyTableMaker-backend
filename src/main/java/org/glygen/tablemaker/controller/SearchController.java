package org.glygen.tablemaker.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.glygen.tablemaker.persistence.SearchResultEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.DatasetRepository;
import org.glygen.tablemaker.persistence.dao.GlycanImageRepository;
import org.glygen.tablemaker.persistence.dao.RetractionRepository;
import org.glygen.tablemaker.persistence.dao.SearchResultRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.glygen.tablemaker.view.DatasetSearchInput;
import org.glygen.tablemaker.view.DatasetSearchResultView;
import org.glygen.tablemaker.view.DatasetView;
import org.glygen.tablemaker.view.Sorting;
import org.glygen.tablemaker.view.SuccessResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;


@RestController
@RequestMapping("/api/search")
public class SearchController {
	
	static Logger logger = org.slf4j.LoggerFactory.getLogger(SearchController.class);
	
	final SearchResultRepository searchResultRepository;
	final DatasetRepository datasetRepository;
	final GlycanImageRepository glycanImageRepository;
	final UserRepository userRepository;
	final RetractionRepository retractionRepository;
	
	@Value("${spring.file.imagedirectory}")
    String imageLocation;
	
	public SearchController(SearchResultRepository searchResultRepository, DatasetRepository datasetRepository, GlycanImageRepository glycanImageRepository, UserRepository userRepository, RetractionRepository retractionRepository) {
		this.searchResultRepository = searchResultRepository;
		this.datasetRepository = datasetRepository;
		this.glycanImageRepository = glycanImageRepository;
		this.userRepository = userRepository;
		this.retractionRepository = retractionRepository;
	}
	
	
	@Operation(summary = "List datasets from the given search")
    @RequestMapping(value="/listDatasetsForSearch", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Datasets retrieved successfully", content = {
            @Content(schema = @Schema(implementation = DatasetSearchResultView.class))}), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<DatasetSearchResultView>> listDatasetsForSearch (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("start") Integer offset,
            @Parameter(required=false, description="limit of the number of datasets to be retrieved", example="10") 
            @RequestParam(value="size", required=false) Integer limit, 
            @Parameter(required=false, description="sorting fields and their orders") 
            @RequestParam(value="sorting", required=false) String sorting, 
            @Parameter(required=true, description="the search query id retrieved earlier by the corresponding search") 
            @RequestParam(value="searchId", required=true) String searchId) {
        DatasetSearchResultView result = new DatasetSearchResultView();
        try {
            if (offset == null)
                offset = 0;
            if (limit == null)
                limit = Integer.MAX_VALUE;;
            
            ObjectMapper mapper = new ObjectMapper();
            
            List<Sorting> sortingList = null;
            List<Order> sortOrders = new ArrayList<>();
            if (sorting != null && !sorting.equals("[]")) {
                try {
                    sortingList = mapper.readValue(sorting, 
                        new TypeReference<ArrayList<Sorting>>() {});
                    for (Sorting s: sortingList) {
                        sortOrders.add(new Order(s.getDesc() ? Direction.DESC: Direction.ASC, s.getId()));
                    }
                } catch (JsonProcessingException e) {
                    throw new InternalError("sorting parameter is invalid " + sorting, e);
                }
            } else {
            	sortOrders.add(new Order(Direction.ASC, "datasetId"));
            }
            
            List<String> matches = null;
            List<DatasetView> searchDatasets = new ArrayList<DatasetView>();
            String idList = null;
            try {
                SearchResultEntity r = searchResultRepository.findBySearchKey(searchId.trim());
                if (r != null) {
                    idList = r.getIdList();
                    result.setInput(new ObjectMapper().readValue(r.getInput(), DatasetSearchInput.class));
                }
            } catch (Exception e) {
                logger.error("Cannot retrieve the search result", e);
            }
            if (idList != null && !idList.isEmpty()) {
                matches = Arrays.asList(idList.split(","));  
            } else if (idList == null) {
                throw new IllegalArgumentException("Given search id does not correspond to a valid search");
            }
            
            int total=0;
            
            if (matches != null) {
                total = matches.size();
                
                List<Long> ids = new ArrayList<>();
                for (String match: matches) {
                	try {
                		ids.add(Long.parseLong(match));
                	} catch (NumberFormatException e) {
                		logger.error("Id list should contain dataset ids", e);
                	}
                }
                
                Page<Dataset> datasets = datasetRepository.findByDatasetIdIn(ids, PageRequest.of(offset, limit, Sort.by(sortOrders)));
                
                for (Dataset set: datasets.getContent()) {
                	searchDatasets.add(DatasetController.createDatasetView(set, null, glycanImageRepository, imageLocation, retractionRepository));
                }
            }
            
            result.setObjects(searchDatasets);
            result.setTotalItems(total);
            result.setFilteredTotal(searchDatasets.size());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot retrieve datasets for search. Reason: " + e.getMessage());
        }
        
        return new ResponseEntity<>(new SuccessResponse<>(result, "dataset search results retrieved"), HttpStatus.OK);
    }
	
	@Operation(summary = "Perform search on datasets that match all of the given criteria")
    @RequestMapping(value="/searchDatasets", method = RequestMethod.POST)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="The search id to be used to retrieve search results", content = {
            @Content(schema = @Schema(implementation = String.class))}), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<String>> searchDatasets (
            @Parameter(required=true, description="search terms") 
            @RequestBody DatasetSearchInput searchInput) {
        
        Map<String, List<String>> searchResultMap = new HashMap<String, List<String>>();
        
        try {
            if (searchInput.getUsername() != null && !searchInput.getUsername().trim().isEmpty()) {
            	// search by owner
            	UserEntity user = userRepository.findByUsernameIgnoreCase(searchInput.getUsername());
            	List<Long> ids = datasetRepository.getAllDatasetIdsByUser(user);
            	List<String> matchedIds = new ArrayList<String>();
                for (Long id: ids) {
                	matchedIds.add(id+"");
                }
                
                searchResultMap.put(searchInput.getUsername().trim().hashCode()+"user", matchedIds);
            }
            
            if (searchInput.getGroup() != null && !searchInput.getGroup().trim().isEmpty()) {
                List<UserEntity> users = userRepository.findAllByGroupNameIgnoreCase(searchInput.getGroup().trim());
                List<String> matchedIds = new ArrayList<String>();
                for (UserEntity user: users) {
                    // search by owner
                	List<Long> ids = datasetRepository.getAllDatasetIdsByUser(user);
                    for (Long id: ids) {
                    	matchedIds.add(id+"");
                    }
                }
                
                searchResultMap.put(searchInput.getGroup().trim().hashCode()+"gr", matchedIds);
            }
            
            if (searchInput.getInstitution() != null && !searchInput.getInstitution().trim().isEmpty()) {
                List<UserEntity> users = userRepository.findAllByAffiliationIgnoreCase(searchInput.getInstitution().trim());
                List<String> matchedIds = new ArrayList<String>();
                for (UserEntity user: users) {
                    // search by owner
                	List<Long> ids = datasetRepository.getAllDatasetIdsByUser(user);
                    for (Long id: ids) {
                    	matchedIds.add(id+"");
                    }
                }
                
                searchResultMap.put(searchInput.getInstitution().trim().hashCode()+"org", matchedIds);
            }
            
            if (searchInput.getDepartment() != null && !searchInput.getDepartment().trim().isEmpty()) {
                List<UserEntity> users = userRepository.findAllByDepartmentIgnoreCase(searchInput.getDepartment().trim());
                List<String> matchedIds = new ArrayList<String>();
                for (UserEntity user: users) {
                    // search by owner
                	List<Long> ids = datasetRepository.getAllDatasetIdsByUser(user);
                    for (Long id: ids) {
                    	matchedIds.add(id+"");
                    }
                }
                
                searchResultMap.put(searchInput.getDepartment().trim().hashCode()+"dept", matchedIds);
            }
            
            if (searchInput.getDatasetName() != null && !searchInput.getDatasetName().trim().isEmpty()) {
            	List<Dataset> datasets = datasetRepository.findByNameContainingIgnoreCase(searchInput.getDatasetName().trim());
            	List<String> matchedIds = new ArrayList<>();
            	for (Dataset d: datasets) {
            		matchedIds.add(d.getDatasetId()+"");
            	}
            	
            	searchResultMap.put(searchInput.getDatasetName().trim().hashCode()+"name", matchedIds);
            }
            
            if (searchInput.getFundingAgency() != null && !searchInput.getFundingAgency().trim().isEmpty()) {
            	List<Long> ids = datasetRepository.getAllDatasetIdsByFundingOrganization(searchInput.getFundingAgency());
            	List<String> matchedIds = new ArrayList<String>();
                for (Long id: ids) {
                	matchedIds.add(id+"");
                }
                
                searchResultMap.put(searchInput.getUsername().trim().hashCode()+"fund", matchedIds);
            }
            
            Set<String> finalMatches = new HashSet<String>();
            int i=0;
            String searchKey = "";
            for (String key: searchResultMap.keySet()) {
                searchKey += key;
                List<String> matches = searchResultMap.get(key);
                if (i == 0)
                    finalMatches.addAll(matches);
                else {
                    // get the intersection
                    finalMatches = matches.stream()
                            .distinct()
                            .filter(finalMatches::contains)
                            .collect(Collectors.toSet());
                    
                }
                i++;
            }
            
            if ((searchInput.getUsername() == null || searchInput.getUsername().trim().isEmpty()) 
                    && (searchInput.getGroup() == null || searchInput.getGroup().trim().isEmpty())
                    && (searchInput.getDepartment() == null || searchInput.getDepartment().trim().isEmpty())
                    && (searchInput.getDatasetName() == null || searchInput.getDatasetName().trim().isEmpty())
                    && (searchInput.getFundingAgency() == null || searchInput.getFundingAgency().trim().isEmpty())
                    && (searchInput.getInstitution() == null || searchInput.getInstitution().trim().isEmpty())) {
                // no restrictions, return all datasets
                searchKey = "alldatasets";
                List<Long> ids = datasetRepository.getAllDatasetIds();
                for (Long m: ids) {
                    finalMatches.add(m+"");
                }
            }
            
            
            if (finalMatches.isEmpty()) {
                // do not save the search results, return an error code
                throw new IllegalArgumentException("No results found");
            }
            
            if (!searchKey.isEmpty()) {
                SearchResultEntity searchResult = new SearchResultEntity();
                searchResult.setSearchKey(searchKey);
                searchResult.setIdList(String.join(",", finalMatches));
                try {
                    searchResult.setInput(new ObjectMapper().writeValueAsString(searchInput));
                } catch (JsonProcessingException e) {
                    logger.warn("could not serialize the search input" + e.getMessage());
                }
                searchResultRepository.save(searchResult);
                return new ResponseEntity<>(new SuccessResponse<>(searchResult.getSearchKey(), " no dataset search results found"), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new SuccessResponse<>(null, " no dataset search results found"), HttpStatus.OK);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot retrieve datasets for search. Reason: " + e.getMessage());
        } 
    }
	
/*	@Operation(summary = "Perform search on datasets that match all of the given criteria")
    @RequestMapping(value="/searchDatasets", method = RequestMethod.POST)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="The search id to be used to retrieve search results", content = {
            @Content(schema = @Schema(implementation = String.class))}), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String searchDatasets (
            @Parameter(required=true, description="search terms") 
            @RequestBody DatasetSearchInput searchInput) {
        
        Map<String, List<String>> searchResultMap = new HashMap<String, List<String>>();
        try {
            if (searchInput.getDatasetName() != null && !searchInput.getDatasetName().trim().isEmpty()) {
            	
                List<SparqlEntity> results = queryHelper.retrieveByLabel(
                        searchInput.getDatasetName().trim(), ArrayDatasetRepositoryImpl.datasetTypePredicate, GlygenArrayRepository.DEFAULT_GRAPH);
                List<String> matchedIds = new ArrayList<String>();
                if (results != null) {
                    for (SparqlEntity r: results) {
                        String m = r.getValue("s");
                        matchedIds.add(m.substring(m.lastIndexOf("/")+1));
                    }
                }
                searchResultMap.put(searchInput.getDatasetName().trim().hashCode()+"n", matchedIds);
            }
            if (searchInput.getPrintedSlideName() != null && !searchInput.getPrintedSlideName().trim().isEmpty()) {
                List<SparqlEntity> results = queryHelper.retrieveDatasetBySlideName(
                        searchInput.getPrintedSlideName().trim(), GlygenArrayRepository.DEFAULT_GRAPH);
                List<String> matchedIds = new ArrayList<String>();
                if (results != null) {
                    for (SparqlEntity r: results) {
                        String m = r.getValue("s");
                        matchedIds.add(m.substring(m.lastIndexOf("/")+1));
                    }
                }
                searchResultMap.put(searchInput.getPrintedSlideName().trim().hashCode()+"s", matchedIds);
            }
            
            if (searchInput.getPmid() != null && !searchInput.getPmid().trim().isEmpty()) {
                List<SparqlEntity> results = queryHelper.retrieveDatasetByPublication(
                        searchInput.getPmid().trim(), GlygenArrayRepository.DEFAULT_GRAPH);
                List<String> matchedIds = new ArrayList<String>();
                if (results != null) {
                    for (SparqlEntity r: results) {
                        String m = r.getValue("s");
                        matchedIds.add(m.substring(m.lastIndexOf("/")+1));
                    }
                }
                searchResultMap.put(searchInput.getPmid().trim().hashCode()+"p", matchedIds);
            }
            
            if (searchInput.getKeyword() != null && !searchInput.getKeyword().trim().isEmpty()) {
                List<SparqlEntity> results = queryHelper.retrieveDatasetByKeyword(
                        searchInput.getKeyword().trim(), GlygenArrayRepository.DEFAULT_GRAPH);
                List<String> matchedIds = new ArrayList<String>();
                if (results != null) {
                    for (SparqlEntity r: results) {
                        String m = r.getValue("s");
                        matchedIds.add(m.substring(m.lastIndexOf("/")+1));
                    }
                }
                searchResultMap.put(searchInput.getKeyword().trim().hashCode()+"k", matchedIds);
            }
            
            Set<String> finalMatches = new HashSet<String>();
            int i=0;
            String searchKey = "";
            for (String key: searchResultMap.keySet()) {
                searchKey += key;
                List<String> matches = searchResultMap.get(key);
                if (i == 0)
                    finalMatches.addAll(matches);
                else {
                    // get the intersection
                    finalMatches = matches.stream()
                            .distinct()
                            .filter(finalMatches::contains)
                            .collect(Collectors.toSet());
                    
                }
                i++;
            }
            
            if ((searchInput.getDatasetName() == null || searchInput.getDatasetName().trim().isEmpty()) 
                    && (searchInput.getPmid() == null || searchInput.getPmid().trim().isEmpty()) 
                    && (searchInput.getKeyword() == null || searchInput.getKeyword().trim().isEmpty()) 
                    && (searchInput.getPrintedSlideName() == null || searchInput.getPrintedSlideName().trim().isEmpty())) {
                // no restrictions, return all datasets
                searchKey = "alldatasets";
                List<String> matches = datasetRepository.getAllDatasets(null);
                for (String m: matches) {
                    finalMatches.add(m.substring(m.lastIndexOf("/")+1));
                }
            }
            
            
            if (finalMatches.isEmpty()) {
                // do not save the search results, return an error code
                ErrorMessage errorMessage = new ErrorMessage("No results found");
                errorMessage.setStatus(HttpStatus.NOT_FOUND.value());
                errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                throw new IllegalArgumentException("No results found", errorMessage);
            }
            
            if (!searchKey.isEmpty()) {
                GlycanSearchResultEntity searchResult = new GlycanSearchResultEntity();
                searchResult.setSequence(searchKey);
                searchResult.setIdList(String.join(",", finalMatches));
                searchResult.setSearchType(DatasetSearchType.GENERAL.name());
                try {
                    searchResult.setInput(new ObjectMapper().writeValueAsString(searchInput));
                } catch (JsonProcessingException e) {
                    logger.warn("could not serialize the search input" + e.getMessage());
                }
                searchResultRepository.save(searchResult);
                return searchResult.getSequence();
            } else {
                return null;
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve datasets for search. Reason: " + e.getMessage());
        } 
    }*/

}
