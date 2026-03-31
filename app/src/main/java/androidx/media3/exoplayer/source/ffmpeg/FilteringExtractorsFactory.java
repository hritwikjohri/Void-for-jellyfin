/*
 * Local shim for FFmpeg filtering that wraps extractor outputs with the
 * Media3 filtering adapter.
 */
package androidx.media3.exoplayer.source.ffmpeg;

import android.net.Uri;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.ffmpeg.FfmpegFilteringExtractorAdapter;
import androidx.media3.extractor.ffmpeg.FfmpegFilteringMode;
import androidx.media3.extractor.ffmpeg.FfmpegFilteringSession;
import androidx.media3.extractor.text.SubtitleParser;
import java.util.List;
import java.util.Map;

@UnstableApi
public final class FilteringExtractorsFactory implements ExtractorsFactory {

  private final ExtractorsFactory delegate;
  private final @FfmpegFilteringMode.Mode int mode;
  private final FfmpegFilteringSession.Factory sessionFactory;

  public FilteringExtractorsFactory(ExtractorsFactory delegate, @FfmpegFilteringMode.Mode int mode) {
    this(delegate, mode, FfmpegFilteringSession.defaultFactory());
  }

  public FilteringExtractorsFactory(
      ExtractorsFactory delegate,
      @FfmpegFilteringMode.Mode int mode,
      FfmpegFilteringSession.Factory sessionFactory) {
    this.delegate = delegate;
    this.mode = mode;
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Extractor[] createExtractors() {
    return wrap(delegate.createExtractors());
  }

  @Override
  public Extractor[] createExtractors(Uri uri, Map<String, List<String>> responseHeaders) {
    return wrap(delegate.createExtractors(uri, responseHeaders));
  }

  @Override
  @Deprecated
  public ExtractorsFactory experimentalSetTextTrackTranscodingEnabled(
      boolean textTrackTranscodingEnabled) {
    delegate.experimentalSetTextTrackTranscodingEnabled(textTrackTranscodingEnabled);
    return this;
  }

  @Override
  public ExtractorsFactory setSubtitleParserFactory(SubtitleParser.Factory subtitleParserFactory) {
    delegate.setSubtitleParserFactory(subtitleParserFactory);
    return this;
  }

  @Override
  public ExtractorsFactory experimentalSetCodecsToParseWithinGopSampleDependencies(
      int codecsToParseWithinGopSampleDependencies) {
    delegate.experimentalSetCodecsToParseWithinGopSampleDependencies(
        codecsToParseWithinGopSampleDependencies);
    return this;
  }

  private Extractor[] wrap(Extractor[] extractors) {
    Extractor[] wrapped = new Extractor[extractors.length];
    for (int i = 0; i < extractors.length; i++) {
      wrapped[i] = new FfmpegFilteringExtractorAdapter(extractors[i], mode, sessionFactory);
    }
    return wrapped;
  }
}
