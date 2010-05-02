package org.jbehave.core.reporters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jbehave.core.reporters.FilePrintStreamFactory.FileConfiguration;

/**
 * <p>
 * A <a href="http://en.wikipedia.org/wiki/Builder_pattern">Builder</a> for
 * {@link StoryReporter}s. It builds a {@link DelegatingStoryReporter}
 * with delegates for a number of formats - mostly file-based ones except
 * {@Format.CONSOLE}. It requires a
 * {@link FilePrintStreamFactory} and provides default delegate instances for
 * each format.
 * </p>
 * <p>
 * To build reporter with default delegates for given formats:
 * <pre>
 * Class&lt;MyStory&gt; storyClass = MyStory.class;
 * StoryPathResolver resolver = new UnderscoredCamelCaseResolver();
 * String storyPath = resolver.resolve(storyClass);
 * FilePrintStreamFactory printStreamFactory = new FilePrintStreamFactory(storyPath);
 * StoryReporter reporter = new StoryReporterBuilder(printStreamFactory)
 * 								.withDefaultFormats().with(HTML).with(TXT).build();
 * </pre> 
 * </p>
 * <p>The builder is configured to build with the {@link Format#STATS} format by default.  To change the default formats
 * the user can override the method:
 * <pre>
 * new StoryReporterBuilder(printStreamFactory){
 *    protected void withDefaultFormats() {
 *      with(Format.STATS);
 *    }
 *  }
 * </pre>
 * </p>
 * <p>The builder configures the file-based reporters to output to the default file directory {@link FileConfiguration#DIRECTORY}.
 * To change the default:
 * <pre>
 * new StoryReporterBuilder(printStreamFactory).outputTo("my-reports").with(HTML).with(TXT).build();
 * </pre>
 * </p> 
 * <p>The builder provides default instances for all reporters.  To change the reporter for a specific instance, 
 * e.g. to report format <b>TXT</b> to <b>.text</b> files and to inject other non-default parameters, 
 * such as keywords for a different locale:
 * <pre>
 * new StoryReporterBuilder(printStreamFactory){
 *   public StoryReporter reporterFor(Format format){
 *       switch (format) {
 *           case TXT:
 *               factory.useConfiguration(new FileConfiguration("text"));
 *               return new PrintStreamStoryReporter(factory.createPrintStream(), new Properties(), new I18nKeywords(Locale), true);
 *            default:
 *               return super.reporterFor(format);
 *   }
 * }
 * </pre>
 */
public class StoryReporterBuilder {

    public enum Format {
        CONSOLE, STATS, TXT, HTML, XML
    }

    protected final FilePrintStreamFactory factory;
    protected Map<Format, StoryReporter> delegates = new HashMap<Format, StoryReporter>();
    private String outputDirectory = new FileConfiguration().getOutputDirectory();
    private boolean outputAbsolute = new FileConfiguration().isOutputDirectoryAbsolute();

    public StoryReporterBuilder(FilePrintStreamFactory factory) {
        this.factory = factory;
    }

    public StoryReporterBuilder withDefaultFormats() {
    	return with(Format.STATS);
    }

    public StoryReporter build() {
        return new DelegatingStoryReporter(delegates.values());
    }

    public StoryReporterBuilder outputTo(String outputDirectory){
        this.outputDirectory = outputDirectory;
        return this;
    }
    
    public StoryReporterBuilder outputAsAbsolute(boolean outputAbsolute) {
        this.outputAbsolute = outputAbsolute;
        return this;
    }
    
    public StoryReporterBuilder with(Format format) {
        delegates.put(format, reporterFor(format));
        return this;
    }

    public StoryReporter reporterFor(Format format) {
        switch (format) {
            case CONSOLE:
                return new PrintStreamOutput();
            case STATS:
                factory.useConfiguration(fileConfiguration("stats"));
                return new PostStoryStatisticsCollector(factory.createPrintStream());
            case TXT:
                factory.useConfiguration(fileConfiguration("txt"));
                return new PrintStreamOutput(factory.createPrintStream());
            case HTML:
                factory.useConfiguration(fileConfiguration("html"));
                return new HtmlOutput(factory.createPrintStream());
            case XML:
                factory.useConfiguration(fileConfiguration("xml"));
                return new XmlOutput(factory.createPrintStream());
            default:
                throw new UnsupportedReporterFormatException(format);
        }
    }
    
    public Map<Format, StoryReporter> getDelegates() {
        return Collections.unmodifiableMap(delegates);
    }

    protected FileConfiguration fileConfiguration(String extension) {
        return new FileConfiguration(outputDirectory, outputAbsolute, extension);
    }

    @SuppressWarnings("serial")
    public static class UnsupportedReporterFormatException extends RuntimeException {

        public UnsupportedReporterFormatException(Format format) {
            super("Building StoryReporter not supported for format " + format);
        }

    }

}
