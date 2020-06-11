package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

	public static final String VIDEO_SVC_PATH = "/video";
	public static final String VIDEO_DATA_PATH = VIDEO_SVC_PATH + "/{id}/data";
	private static final AtomicLong currentId = new AtomicLong(0L);

	// An in-memory list that the servlet uses to store the
	// videos that are sent to it by clients
	private VideoFileManager videoDataMgr;
	private Map<Long, Video> videos = new HashMap<Long, Video>();

	@RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return videos.values();
	}

	@RequestMapping(value = VIDEO_DATA_PATH, method = RequestMethod.GET)
	public void getData(
			@PathVariable("id") long id,
			HttpServletResponse response
	) throws IOException {
		if (videoDataMgr == null) {
			videoDataMgr = VideoFileManager.get();
		}
		try {
			if(!videoDataMgr.hasVideoData(videos.get(id)))
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			else
				videoDataMgr.copyVideoData(videos.get(id), response.getOutputStream());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.POST)
	public @ResponseBody Video addVideoMetadata(
			@RequestBody Video v,
			HttpServletRequest request,
			HttpServletResponse response
	) throws IOException {
		try{
			v.setId(currentId.incrementAndGet());
			v.setDataUrl(getDataUrl(v.getId()));
			videos.put(v.getId(), v);

		} catch (Exception e){
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		return v;
	}

	@RequestMapping(value = VIDEO_DATA_PATH, method = RequestMethod.POST)
	public @ResponseBody VideoStatus addVideoData(
			@PathVariable("id") long id,
			@RequestParam MultipartFile data,
			HttpServletResponse response
	) throws IOException {
		if (videoDataMgr == null)
			videoDataMgr = VideoFileManager.get();
		try {
			videoDataMgr.saveVideoData(videos.get(id), data.getInputStream());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		return new VideoStatus(VideoStatus.VideoState.READY);
	}

	private String getDataUrl(long videoId){
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}


	private String getUrlBaseForLocalServer() {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String base = "http://" + request.getServerName()
				+ ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
		return base;
	}

}