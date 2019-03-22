package ch.puzzle.cncfmeetupdemo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@RestController
@RequestMapping(value = "/api")
public class ClickController {

	private final Counter meetupThumbsUpCounter;
	private final Counter meetupThumbsDownCounter;
	private final Counter meetupThumbs;
	private final Counter meetupStars;
	private final Counter meetupStarsVotes;
	
	private final Map<String, AtomicInteger> gauges;
	private final MeterRegistry meterRegistry;
	

	@Autowired
	public ClickController(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.gauges = new HashMap<>();
		
		this.meetupThumbsUpCounter = meterRegistry.counter("meetup.thumbs.up.count");
		this.meetupThumbsDownCounter = meterRegistry.counter("meetup.thumbs.down.count");
		this.meetupThumbs = meterRegistry.counter("meetup.thumbs.total");
		this.meetupStars = meterRegistry.counter("meetup.stars.total");
		this.meetupStarsVotes = meterRegistry.counter("meetup.stars.votes.total");
	}
	

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping(value="/up/{voter}")
	public void postUp(@PathVariable("voter") String voter) {
		if(!gauges.containsKey(voter)) {
			gauges.put(voter, meterRegistry.gauge("meetup.thumbs", Arrays.asList(Tag.of("thumb", voter)), new AtomicInteger(0)));
		}
		gauges.get(voter).incrementAndGet();
		
		meetupThumbsUpCounter.increment();
		meetupThumbs.increment();
	}
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping(value="/down/{voter}")
	public void postDown(@PathVariable("voter") String voter) {
		if(!gauges.containsKey(voter)) {
			gauges.put(voter, meterRegistry.gauge("meetup.thumbs", Arrays.asList(Tag.of("thumb", voter)), new AtomicInteger(0)));
		}
		
		gauges.get(voter).decrementAndGet();

		meetupThumbsDownCounter.increment();
		meetupThumbs.increment();
	}
	
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping(value="/stars/{stars}")
	public void postStars(@PathVariable("stars") int stars) {
		meetupStars.increment(stars);
		meetupStarsVotes.increment();
	}

	@GetMapping
	public Metrics getAll() {
		return new Metrics(meetupThumbsUpCounter.count(), meetupThumbsDownCounter.count(), meetupThumbs.count(), meetupStars.count(), meetupStarsVotes.count(), gauges);
	}
	
	public class Metrics {
		
		private double thumbsUp;
		private double thumbsDown;
		private double thumbs;
		private double meetupStars;
		private double meetupStarsVotes;
		private Map<String, AtomicInteger> gauges;
		public Metrics(double thumbsUp, double thumbsDown, double thumbs, double meetupStars, double meetupStarsVotes, Map<String, AtomicInteger> gauges) {
			super();
			this.thumbsUp = thumbsUp;
			this.thumbsDown = thumbsDown;
			this.thumbs = thumbs;
			this.gauges = gauges;
			this.meetupStars = meetupStars;
			this.meetupStarsVotes = meetupStarsVotes;
		}
		public double getThumbsUp() {
			return thumbsUp;
		}
		public double getThumbsDown() {
			return thumbsDown;
		}
		public double getClicks() {
			return thumbs;
		}
		public Map<String, AtomicInteger> getGauges() {
			return gauges;
		}
		public double getMeetupStars() {
			return meetupStars;
		}
		public void setMeetupStars(double meetupStars) {
			this.meetupStars = meetupStars;
		}
		public double getMeetupStarsVotes() {
			return meetupStarsVotes;
		}
		public void setMeetupStarsVotes(double meetupStarsVotes) {
			this.meetupStarsVotes = meetupStarsVotes;
		}
	}
}
