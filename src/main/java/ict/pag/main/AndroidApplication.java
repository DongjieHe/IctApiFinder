package ict.pag.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import heros.solver.Pair;
import soot.G;
import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.callbacks.AbstractCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.CallbackDefinition;
import soot.jimple.infoflow.android.callbacks.CallbackDefinition.CallbackType;
import soot.jimple.infoflow.android.callbacks.DefaultCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.FastCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.filters.AlienFragmentFilter;
import soot.jimple.infoflow.android.callbacks.filters.AlienHostComponentFilter;
import soot.jimple.infoflow.android.callbacks.filters.ApplicationCallbackFilter;
import soot.jimple.infoflow.android.config.SootConfigForAndroid;
import soot.jimple.infoflow.android.iccta.IccInstrumenter;
import soot.jimple.infoflow.android.iccta.MessengerInstrumenter;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.StringResource;
import soot.jimple.infoflow.android.resources.LayoutControl;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory;
import soot.jimple.infoflow.data.pathBuilders.IPathBuilderFactory;
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointCreator;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.memory.FlowDroidMemoryWatcher;
import soot.jimple.infoflow.memory.FlowDroidTimeoutWatcher;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.source.ISourceSinkManager;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.options.Options;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

public class AndroidApplication {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private MultiMap<SootClass, CallbackDefinition> callbackMethods = new HashMultiMap<>();
	private MultiMap<SootClass, SootClass> fragmentClasses = new HashMultiMap<>();

	protected InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();

	private Set<SootClass> entrypoints = null;
	private Set<String> callbackClasses = null;
	private SootMethod dummyMainMethod = null;

	protected ARSCFileParser resources = null;
	protected ProcessManifest manifest = null;

	private final String androidJar;
	private final boolean forceAndroidJar;
	private final String apkFileLocation;
	private final String additionalClasspath;

	private ISourceSinkManager sourceSinkManager = null;

	private IInfoflowConfig sootConfig = new SootConfigForAndroid();
	private BiDirICFGFactory cfgFactory = null;

	private String callbackFile = "AndroidCallbacks.txt";
	private SootClass scView = null;

	private Set<PreAnalysisHandler> preprocessors = new HashSet<>();
	private Set<ResultsAvailableHandler> resultsAvailableHandlers = new HashSet<>();

	public int getMinSdkVersion() {
		int minLevel = manifest.getMinSdkVersion();
		if (minLevel == -1) {
			minLevel = 1;
		}
		return minLevel;
	}

	public int targetSdkVersion() {
		int tgtLevel = manifest.targetSdkVersion();
		int minLevel = manifest.getMinSdkVersion();
		if (tgtLevel == -1) {
			if (minLevel != -1) {
				tgtLevel = minLevel;
			} else {
				tgtLevel = 1;
			}
		}
		return tgtLevel;
	}

	public AndroidApplication(String androidJar, String apkFileLocation) {
		File f = new File(androidJar);
		this.forceAndroidJar = f.isFile();

		this.androidJar = androidJar;
		this.apkFileLocation = apkFileLocation;
		this.additionalClasspath = "";

		// Start a new Soot instance
		G.reset();
		initializeSoot(true);
	}

	public String getAppName() {
		String appName = apkFileLocation.substring(apkFileLocation.lastIndexOf("/") + 1);
		return appName;
	}

	/**
	 * Gets the set of classes containing entry point methods for the lifecycle
	 *
	 * @return The set of classes containing entry point methods for the lifecycle
	 */
	public Set<SootClass> getEntrypointClasses() {
		return entrypoints;
	}

	/**
	 * Prints list of classes containing entry points to stdout
	 */
	public void printEntrypoints() {
		if (this.entrypoints == null)
			System.out.println("Entry points not initialized");
		else {
			System.out.println("Classes containing entry points:");
			for (SootClass sc : entrypoints)
				System.out.println("\t" + sc.getName());
			System.out.println("End of Entrypoints");
		}
	}

	/**
	 * Sets the class names of callbacks. If this value is null, it automatically loads the names from
	 * AndroidCallbacks.txt as the default behavior.
	 *
	 * @param callbackClasses
	 *            The class names of callbacks or null to use the default file.
	 */
	public void setCallbackClasses(Set<String> callbackClasses) {
		this.callbackClasses = callbackClasses;
	}

	public Set<String> getCallbackClasses() {
		return callbackClasses;
	}

	/**
	 * Parses common app resources such as the manifest file
	 *
	 * @throws IOException
	 *             Thrown if the given source/sink file could not be read.
	 * @throws XmlPullParserException
	 *             Thrown if the Android manifest file could not be read.
	 */
	private void parseAppResources() throws IOException, XmlPullParserException {
		// To look for callbacks, we need to start somewhere. We use the Android
		// lifecycle methods for this purpose.
		this.manifest = new ProcessManifest(apkFileLocation);
		this.entrypoints = new HashSet<>();
		for (String className : manifest.getEntryPointClasses())
			this.entrypoints.add(Scene.v().getSootClassUnsafe(className));

		// Parse the resource file
		long beforeARSC = System.nanoTime();
		this.resources = new ARSCFileParser();
		this.resources.parse(apkFileLocation);
		logger.info("ARSC file parsing took " + (System.nanoTime() - beforeARSC) / 1E9 + " seconds");
	}

	/**
	 * Calculates the sets of sources, sinks, entry points, and callbacks methods for the entry point in the given APK
	 * file.
	 *
	 * @param sourcesAndSinks
	 *            A provider from which the analysis can obtain the list of sources and sinks
	 * @param entryPoint
	 *            The entry point for which to calculate the callbacks. Pass null to calculate callbacks for all entry
	 *            points.
	 * @throws IOException
	 *             Thrown if the given source/sink file could not be read.
	 * @throws XmlPullParserException
	 *             Thrown if the Android manifest file could not be read.
	 */
	private void calculateCallbacks(SootClass entryPoint) throws IOException, XmlPullParserException {
		// Add the callback methods
		LayoutFileParser lfp = null;
		if (config.getEnableCallbacks()) {
			if (callbackClasses != null && callbackClasses.isEmpty()) {
				logger.warn("Callback definition file is empty, disabling callbacks");
			} else {
				lfp = new LayoutFileParser(this.manifest.getPackageName(), this.resources);
				switch (config.getCallbackAnalyzer()) {
				case Fast:
					calculateCallbackMethodsFast(lfp, entryPoint);
					break;
				case Default:
					calculateCallbackMethods(lfp, entryPoint);
					break;
				default:
					throw new RuntimeException("Unknown callback analyzer");
				}
			}
		} else if (!config.getUseExistingSootInstance()) {
			// Create the new iteration of the main method
			createMainMethod(null);
			constructCallgraphInternal();
		}

		logger.info("Entry point calculation done.");
	}

	/**
	 * Triggers the callgraph construction in Soot
	 */
	private void constructCallgraphInternal() {
		// Do we need ICC instrumentation?
		IccInstrumenter iccInstrumenter = null;
		if (config.isIccEnabled()) {
			iccInstrumenter = new IccInstrumenter(config.getIccModel());
			iccInstrumenter.onBeforeCallgraphConstruction();

			// To support Messenger-based ICC
			MessengerInstrumenter msgInstrumenter = new MessengerInstrumenter();
			msgInstrumenter.onBeforeCallgraphConstruction();
		}

		// Run the preprocessors
		for (PreAnalysisHandler handler : this.preprocessors)
			handler.onBeforeCallgraphConstruction();

		// Make sure that we don't have any weird leftovers
		releaseCallgraph();

		// Construct the actual callgraph
		logger.info("Constructing the callgraph...");
		PackManager.v().getPack("cg").apply();

		// ICC instrumentation
		if (iccInstrumenter != null)
			iccInstrumenter.onAfterCallgraphConstruction();

		// Run the preprocessors
		for (PreAnalysisHandler handler : this.preprocessors)
			handler.onAfterCallgraphConstruction();

		// Make sure that we have a hierarchy
		Scene.v().getOrMakeFastHierarchy();
	}

	/**
	 * Calculates the set of callback methods declared in the XML resource files or the app's source code
	 *
	 * @param lfp
	 *            The layout file parser to be used for analyzing UI controls
	 * @param component
	 *            The Android component for which to compute the callbacks. Pass null to compute callbacks for all
	 *            components.
	 * @throws IOException
	 *             Thrown if a required configuration cannot be read
	 */
	private void calculateCallbackMethods(LayoutFileParser lfp, SootClass component) throws IOException {
		// Load the APK file
		if (config.getUseExistingSootInstance()) {
			releaseCallgraph();

			// Make sure that we don't have any leftovers from previous runs
			PackManager.v().getPack("wjtp").remove("wjtp.lfp");
			PackManager.v().getPack("wjtp").remove("wjtp.ajc");
		}

		// Get the classes for which to find callbacks
		Set<SootClass> entryPointClasses = getComponentsToAnalyze(component);

		// Collect the callback interfaces implemented in the app's
		// source code. Note that the filters should know all components to
		// filter out callbacks even if the respective component is only
		// analyzed later.
		AbstractCallbackAnalyzer jimpleClass = callbackClasses == null
				? new DefaultCallbackAnalyzer(config, entryPointClasses, callbackFile)
				: new DefaultCallbackAnalyzer(config, entryPointClasses, callbackClasses);
		jimpleClass.addCallbackFilter(new AlienHostComponentFilter(entrypoints));
		jimpleClass.addCallbackFilter(new ApplicationCallbackFilter(entrypoints));
		jimpleClass.collectCallbackMethods();

		// Find the user-defined sources in the layout XML files. This
		// only needs to be done once, but is a Soot phase.
		lfp.parseLayoutFile(apkFileLocation);

		// Watch the callback collection algorithm's memory consumption
		FlowDroidMemoryWatcher memoryWatcher = null;
		FlowDroidTimeoutWatcher timeoutWatcher = null;
		if (jimpleClass instanceof IMemoryBoundedSolver) {
			memoryWatcher = new FlowDroidMemoryWatcher();
			memoryWatcher.addSolver((IMemoryBoundedSolver) jimpleClass);

			// Make sure that we don't spend too much time in the callback
			// analysis
			if (config.getCallbackAnalysisTimeout() > 0) {
				timeoutWatcher = new FlowDroidTimeoutWatcher(config.getCallbackAnalysisTimeout());
				timeoutWatcher.addSolver((IMemoryBoundedSolver) jimpleClass);
				timeoutWatcher.start();
			}
		}

		try {
			int depthIdx = 0;
			boolean hasChanged = true;
			boolean isInitial = true;
			while (hasChanged) {
				hasChanged = false;

				// Check whether the solver has been aborted in the meantime
				if (jimpleClass instanceof IMemoryBoundedSolver) {
					if (((IMemoryBoundedSolver) jimpleClass).isKilled())
						break;
				}

				// Create the new iteration of the main method
				createMainMethod(component);

				// Since the gerenation of the main method can take some time,
				// we check again whether we need to stop.
				if (jimpleClass instanceof IMemoryBoundedSolver) {
					if (((IMemoryBoundedSolver) jimpleClass).isKilled())
						break;
				}

				// Reset the callgraph
				if (!isInitial || config.getUseExistingSootInstance()) {
					releaseCallgraph();
				}

				if (!isInitial) {
					// Some callback analyzers need to explicitly update their
					// state for every new dummy main method
					jimpleClass.collectCallbackMethodsIncremental();

					// We only want to parse the layout files once
					PackManager.v().getPack("wjtp").remove("wjtp.lfp");
				}
				isInitial = false;

				// Run the soot-based operations
				constructCallgraphInternal();
				PackManager.v().getPack("wjtp").apply();

				// Creating all callgraph takes time and memory. Check whether
				// the solver has been aborted in the meantime
				if (jimpleClass instanceof IMemoryBoundedSolver) {
					if (((IMemoryBoundedSolver) jimpleClass).isKilled()) {
						logger.warn("Aborted callback collection because of low memory");
						break;
					}
				}

				// Collect the results of the soot-based phases
				if (this.callbackMethods.putAll(jimpleClass.getCallbackMethods()))
					hasChanged = true;

				if (entrypoints.addAll(jimpleClass.getDynamicManifestComponents()))
					hasChanged = true;

				// Collect the XML-based callback methods
				if (collectXmlBasedCallbackMethods(lfp, jimpleClass))
					hasChanged = true;

				// Avoid callback overruns. If we are beyond the callback limit
				// for one entry point, we may not collect any further callbacks
				// for that entry point.
				if (config.getMaxCallbacksPerComponent() > 0) {
					for (Iterator<SootClass> componentIt = this.callbackMethods.keySet().iterator(); componentIt
							.hasNext();) {
						SootClass callbackComponent = componentIt.next();
						if (this.callbackMethods.get(callbackComponent).size() > config.getMaxCallbacksPerComponent()) {
							componentIt.remove();
							jimpleClass.excludeEntryPoint(callbackComponent);
						}
					}
				}

				// Check depth limiting
				depthIdx++;
				if (config.getMaxAnalysisCallbackDepth() > 0 && depthIdx >= config.getMaxAnalysisCallbackDepth())
					break;
			}
		} finally {
			// Shut down the watchers
			if (timeoutWatcher != null)
				timeoutWatcher.stop();
			if (memoryWatcher != null)
				memoryWatcher.close();
		}

		// Filter out callbacks that belong to fragments that are not used by
		// the host activity
		AlienFragmentFilter fragmentFilter = new AlienFragmentFilter(invertMap(fragmentClasses));
		fragmentFilter.reset();
		for (Iterator<Pair<SootClass, CallbackDefinition>> cbIt = this.callbackMethods.iterator(); cbIt.hasNext();) {
			Pair<SootClass, CallbackDefinition> pair = cbIt.next();

			// Check whether the filter accepts the given mapping
			if (!fragmentFilter.accepts(pair.getO1(), pair.getO2().getTargetMethod()))
				cbIt.remove();
			else if (!fragmentFilter.accepts(pair.getO1(), pair.getO2().getTargetMethod().getDeclaringClass())) {
				cbIt.remove();
			}
		}

		// Avoid callback overruns
		if (config.getMaxCallbacksPerComponent() > 0) {
			for (Iterator<SootClass> componentIt = this.callbackMethods.keySet().iterator(); componentIt.hasNext();) {
				SootClass callbackComponent = componentIt.next();
				if (this.callbackMethods.get(callbackComponent).size() > config.getMaxCallbacksPerComponent())
					componentIt.remove();
			}
		}

		// Make sure that we don't retain any weird Soot phases
		PackManager.v().getPack("wjtp").remove("wjtp.lfp");
		PackManager.v().getPack("wjtp").remove("wjtp.ajc");

		// Warn the user if we had to abort the callback analysis early
		if (jimpleClass instanceof IMemoryBoundedSolver) {
			if (((IMemoryBoundedSolver) jimpleClass).isKilled())
				logger.warn("Callback analysis aborted early due to time or memory exhaustion");
		}
	}

	/**
	 * Inverts the given {@link MultiMap}. The keys become values and vice versa
	 *
	 * @param original
	 *            The map to invert
	 * @return An inverted copy of the given map
	 */
	private <K, V> MultiMap<K, V> invertMap(MultiMap<V, K> original) {
		MultiMap<K, V> newTag = new HashMultiMap<>();
		for (V key : original.keySet())
			for (K value : original.get(key))
				newTag.put(value, key);
		return newTag;
	}

	/**
	 * Releases the callgraph and all intermediate objects associated with it
	 */
	private void releaseCallgraph() {
		Scene.v().releaseCallGraph();
		Scene.v().releasePointsToAnalysis();
		Scene.v().releaseReachableMethods();
		G.v().resetSpark();
	}

	/**
	 * Collects the XML-based callback methods, e.g., Button.onClick() declared in layout XML files
	 *
	 * @param lfp
	 *            The layout file parser
	 * @param jimpleClass
	 *            The analysis class that gives us a mapping between layout IDs and components
	 * @return True if at least one new callback method has been added, otherwise false
	 */
	private boolean collectXmlBasedCallbackMethods(LayoutFileParser lfp, AbstractCallbackAnalyzer jimpleClass) {
		// Collect the XML-based callback methods
		boolean hasNewCallback = false;
		for (final SootClass callbackClass : jimpleClass.getLayoutClasses().keySet()) {
			if (jimpleClass.isExcludedEntryPoint(callbackClass))
				continue;

			Set<Integer> classIds = jimpleClass.getLayoutClasses().get(callbackClass);
			for (Integer classId : classIds) {
				AbstractResource resource = this.resources.findResource(classId);
				if (resource instanceof StringResource) {
					final String layoutFileName = ((StringResource) resource).getValue();

					// Add the callback methods for the given class
					Set<String> callbackMethods = lfp.getCallbackMethods().get(layoutFileName);
					if (callbackMethods != null) {
						for (String methodName : callbackMethods) {
							final String subSig = "void " + methodName + "(android.view.View)";

							// The callback may be declared directly in the
							// class or in one of the superclasses
							SootClass currentClass = callbackClass;
							while (true) {
								SootMethod callbackMethod = currentClass.getMethodUnsafe(subSig);
								if (callbackMethod != null) {
									if (this.callbackMethods.put(callbackClass,
											new CallbackDefinition(callbackMethod, CallbackType.Widget)))
										hasNewCallback = true;
									break;
								}
								if (!currentClass.hasSuperclass()) {
									logger.error("Callback method " + methodName + " not found in class "
											+ callbackClass.getName());
									break;
								}
								currentClass = currentClass.getSuperclass();
							}
						}
					}

					// Add the fragments for this class
					Set<SootClass> fragments = lfp.getFragments().get(layoutFileName);
					if (fragments != null)
						for (SootClass fragment : fragments)
							if (fragmentClasses.put(callbackClass, fragment))
								hasNewCallback = true;

					// For user-defined views, we need to emulate their callbacks
					Set<LayoutControl> controls = lfp.getUserControls().get(layoutFileName);
					if (controls != null) {
						for (LayoutControl lc : controls)
							if (!SystemClassHandler.isClassInSystemPackage(lc.getViewClass().getName()))
								registerCallbackMethodsForView(callbackClass, lc);
					}
				} else
					logger.error("Unexpected resource type for layout class");
			}
		}

		// Collect the fragments, merge the fragments created in the code with
		// those declared in Xml files
		if (fragmentClasses.putAll(jimpleClass.getFragmentClasses()))
			hasNewCallback = true;

		return hasNewCallback;
	}

	/**
	 * Calculates the set of callback methods declared in the XML resource files or the app's source code. This method
	 * prefers performance over precision and scans the code including unreachable methods.
	 *
	 * @param lfp
	 *            The layout file parser to be used for analyzing UI controls
	 * @param entryPoint
	 *            The entry point for which to calculate the callbacks. Pass null to calculate callbacks for all entry
	 *            points.
	 * @throws IOException
	 *             Thrown if a required configuration cannot be read
	 */
	private void calculateCallbackMethodsFast(LayoutFileParser lfp, SootClass component) throws IOException {
		// We need a running Soot instance
		if (config.getUseExistingSootInstance()) {
			releaseCallgraph();
		}

		// Construct the current callgraph
		createMainMethod(component);
		constructCallgraphInternal();

		// Get the classes for which to find callbacks
		Set<SootClass> entryPointClasses = getComponentsToAnalyze(component);

		// Collect the callback interfaces implemented in the app's
		// source code
		AbstractCallbackAnalyzer jimpleClass = callbackClasses == null
				? new FastCallbackAnalyzer(config, entryPointClasses, callbackFile)
				: new FastCallbackAnalyzer(config, entryPointClasses, callbackClasses);
		jimpleClass.collectCallbackMethods();

		// Collect the results
		this.callbackMethods.putAll(jimpleClass.getCallbackMethods());
		this.entrypoints.addAll(jimpleClass.getDynamicManifestComponents());

		// Find the user-defined sources in the layout XML files. This
		// only needs to be done once, but is a Soot phase.
		lfp.parseLayoutFileDirect(apkFileLocation);

		// Collect the XML-based callback methods
		collectXmlBasedCallbackMethods(lfp, jimpleClass);

		// Construct the final callgraph
		releaseCallgraph();
		createMainMethod(component);
		constructCallgraphInternal();
	}

	/**
	 * Registers the callback methods in the given layout control so that they are included in the dummy main method
	 *
	 * @param callbackClass
	 *            The class with which to associate the layout callbacks
	 * @param lc
	 *            The layout control whose callbacks are to be associated with the given class
	 */
	private void registerCallbackMethodsForView(SootClass callbackClass, LayoutControl lc) {
		// Ignore system classes
		if (SystemClassHandler.isClassInSystemPackage(callbackClass.getName()))
			return;

		// Get common Android classes
		if (scView == null)
			scView = Scene.v().getSootClass("android.view.View");

		// Check whether the current class is actually a view
		if (!Scene.v().getOrMakeFastHierarchy().canStoreType(lc.getViewClass().getType(), scView.getType()))
			return;

		// There are also some classes that implement interesting callback methods.
		// We model this as follows: Whenever the user overwrites a method in an
		// Android OS class, we treat it as a potential callback.
		SootClass sc = lc.getViewClass();
		Set<String> systemMethods = new HashSet<String>(10000);
		for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOf(sc)) {
			if (parentClass.getName().startsWith("android."))
				for (SootMethod sm : parentClass.getMethods())
					if (!sm.isConstructor())
						systemMethods.add(sm.getSubSignature());
		}

		// Scan for methods that overwrite parent class methods
		for (SootMethod sm : sc.getMethods())
			if (!sm.isConstructor())
				if (systemMethods.contains(sm.getSubSignature()))
					// This is a real callback method
					this.callbackMethods.put(callbackClass, new CallbackDefinition(sm, CallbackType.Widget));
	}

	/**
	 * Creates the main method based on the current callback information, injects it into the Soot scene.
	 *
	 * @param The
	 *            class name of a component to create a main method containing only that component, or null to create
	 *            main method for all components
	 */
	private void createMainMethod(SootClass component) {
		// Always update the entry point creator to reflect the newest set
		// of callback methods
		dummyMainMethod = createEntryPointCreator(component).createDummyMain();
		Scene.v().setEntryPoints(Collections.singletonList(dummyMainMethod));
		if (!dummyMainMethod.getDeclaringClass().isInScene())
			Scene.v().addClass(dummyMainMethod.getDeclaringClass());

		// addClass() declares the given class as a library class. We need to
		// fix this.
		dummyMainMethod.getDeclaringClass().setApplicationClass();
	}

	/**
	 * Builds the classpath for this analysis
	 *
	 * @return The classpath to be used for the taint analysis
	 */
	private String getClasspath() {
		String classpath = forceAndroidJar ? androidJar : Scene.v().getAndroidJarPath(androidJar, apkFileLocation);
		if (this.additionalClasspath != null && !this.additionalClasspath.isEmpty())
			classpath += File.pathSeparator + this.additionalClasspath;
		logger.debug("soot classpath: " + classpath);
		return classpath;
	}

	/**
	 * Initializes soot for running the soot-based phases of the application metadata analysis
	 *
	 * @param constructCallgraph
	 *            True if a callgraph shall be constructed, otherwise false
	 */
	private void initializeSoot(boolean constructCallgraph) {
		// Clean up any old Soot instance we may have
		G.reset();

		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_process_multiple_dex(true);
		Options.v().set_output_format(Options.output_format_none);
		Options.v().set_whole_program(constructCallgraph);
		Options.v().set_process_dir(Collections.singletonList(apkFileLocation));
		if (forceAndroidJar)
			Options.v().set_force_android_jar(androidJar);
		else
			Options.v().set_android_jars(androidJar);
		Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
		Options.v().set_keep_line_number(true);
		Options.v().set_keep_offset(false);
		Options.v().set_throw_analysis(Options.throw_analysis_dalvik);

		// Set the Soot configuration options. Note that this will needs to be
		// done before we compute the classpath.
		if (sootConfig != null)
			sootConfig.setSootOptions(Options.v());

		Options.v().set_soot_classpath(getClasspath());
		Main.v().autoSetOptions();

		// Configure the callgraph algorithm
		if (constructCallgraph) {
			switch (config.getCallgraphAlgorithm()) {
			case AutomaticSelection:
			case SPARK:
				Options.v().setPhaseOption("cg.spark", "on");
				break;
			case GEOM:
				Options.v().setPhaseOption("cg.spark", "on");
				AbstractInfoflow.setGeomPtaSpecificOptions();
				break;
			case CHA:
				Options.v().setPhaseOption("cg.cha", "on");
				break;
			case RTA:
				Options.v().setPhaseOption("cg.spark", "on");
				Options.v().setPhaseOption("cg.spark", "rta:true");
				Options.v().setPhaseOption("cg.spark", "on-fly-cg:false");
				break;
			case VTA:
				Options.v().setPhaseOption("cg.spark", "on");
				Options.v().setPhaseOption("cg.spark", "vta:true");
				break;
			default:
				throw new RuntimeException("Invalid callgraph algorithm");
			}
		}
		if (config.getEnableReflection())
			Options.v().setPhaseOption("cg", "types-for-invoke:true");

		// Load whatever we need
		Scene.v().loadNecessaryClasses();

		List<String> classInDexes = SourceLocator.v().getClassesUnder(this.apkFileLocation);
		for (String className : classInDexes) {
			SootClass c = Scene.v().loadClass(className, SootClass.BODIES);
			c.setApplicationClass();
		}
		// Make sure that we have valid Jimple bodies
		PackManager.v().getPack("wjpp").apply();

		// Patch the callgraph to support additional edges. We do this now,
		// because during callback discovery, the context-insensitive callgraph algorithm would
		// flood us with invalid edges.
		LibraryClassPatcher patcher = new LibraryClassPatcher();
		patcher.patchLibraries();
	}

	/**
	 * Constructs a callgraph only without running the actual data flow analysis. If you want to run a data flow
	 * analysis, do not call this method. Instead, call runInfoflow() directly, which will take care all necessary
	 * prerequisites.
	 */
	public void constructCallgraph() {
		config.setTaintAnalysisEnabled(false);
		// Reset our object state
		this.dummyMainMethod = null;

		// Perform some sanity checks on the configuration
		if (config.getEnableLifecycleSources() && config.isIccEnabled()) {
			logger.warn("ICC model specified, automatically disabling lifecycle sources");
			config.setEnableLifecycleSources(false);
		}

		// Perform basic app parsing
		try {
			parseAppResources();
		} catch (IOException | XmlPullParserException e) {
			logger.error("Callgraph construction failed: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Callgraph construction failed", e);
		}

		// Create the data flow tracker
		InPlaceInfoflow info = createInfoflow();

		// In one-component-at-a-time, we do not have a single entry point
		// creator
		List<SootClass> entrypointWorklist;
		if (config.getOneComponentAtATime())
			entrypointWorklist = new ArrayList<SootClass>(entrypoints);
		else {
			entrypointWorklist = new ArrayList<>();
			SootClass dummyEntrypoint;
			if (Scene.v().containsClass("dummy"))
				dummyEntrypoint = Scene.v().getSootClass("dummy");
			else
				dummyEntrypoint = new SootClass("dummy");
			entrypointWorklist.add(dummyEntrypoint);
		}

		// For every entry point (or the dummy entry point which stands for all
		// entry points at once), run the data flow analysis
		while (!entrypointWorklist.isEmpty()) {
			SootClass entrypoint = entrypointWorklist.remove(0);

			// Perform basic app parsing
			try {
				if (config.getOneComponentAtATime())
					calculateCallbacks(entrypoint);
				else
					calculateCallbacks(null);
			} catch (IOException | XmlPullParserException e) {
				logger.error("Callgraph construction failed: " + e.getMessage());
				e.printStackTrace();
				throw new RuntimeException("Callgraph construction failed", e);
			}

			// Create a new entry point and compute the flows in it. If we
			// analyze all components together, we do not need a new callgraph,
			// but can reuse the one from the callback collection phase.
			if (config.getOneComponentAtATime()) {
				createMainMethod(entrypoint);
				constructCallgraphInternal();
			}
			info.runAnalysis(sourceSinkManager, dummyMainMethod);

			// We don't need the computed callbacks anymore
			this.callbackMethods.clear();
			this.fragmentClasses.clear();
		}
	}

	/**
	 * Specialized {@link Infoflow} class that allows the data flow analysis to be run inside an existing Soot instance
	 *
	 * @author Steven Arzt
	 *
	 */
	private static class InPlaceInfoflow extends Infoflow {
		public InPlaceInfoflow(String androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory,
				IPathBuilderFactory pathBuilderFactory) {
			super(androidPath, forceAndroidJar, icfgFactory, pathBuilderFactory);
		}

		public void runAnalysis(final ISourceSinkManager sourcesSinks, SootMethod entryPoint) {
			this.dummyMainMethod = entryPoint;
			super.runAnalysis(sourcesSinks);
		}

	}

	/**
	 * Instantiates and configures the data flow engine
	 *
	 * @return A properly configured instance of the {@link Infoflow} class
	 */
	private InPlaceInfoflow createInfoflow() {
		// Initialize and configure the data flow tracker
		InPlaceInfoflow info = new InPlaceInfoflow(androidJar, forceAndroidJar, cfgFactory,
				new DefaultPathBuilderFactory(config.getPathBuilder(), config.getComputeResultPaths()));
		info.setConfig(config);
		info.setSootConfig(sootConfig);
		for (ResultsAvailableHandler handler : resultsAvailableHandlers)
			info.addResultsAvailableHandler(handler);

		return info;
	}

	/**
	 * Creates the {@link AndroidEntryPointCreator} instance which will later create the dummy main method for the
	 * analysis
	 *
	 * @param component
	 *            The single component to include in the dummy main method. Pass null to include all components in the
	 *            dummy main method.
	 * @return The {@link AndroidEntryPointCreator} responsible for generating the dummy main method
	 */
	private AndroidEntryPointCreator createEntryPointCreator(SootClass component) {
		Set<SootClass> components = getComponentsToAnalyze(component);
		AndroidEntryPointCreator entryPointCreator = new AndroidEntryPointCreator(components);

		MultiMap<SootClass, SootMethod> callbackMethodSigs = new HashMultiMap<>();
		if (component == null) {
			// Get all callbacks for all components
			for (SootClass sc : this.callbackMethods.keySet()) {
				Set<CallbackDefinition> callbackDefs = this.callbackMethods.get(sc);
				if (callbackDefs != null)
					for (CallbackDefinition cd : callbackDefs)
						callbackMethodSigs.put(sc, cd.getTargetMethod());
			}
		} else {
			// Get the callbacks for the current component only
			for (SootClass sc : components) {
				Set<CallbackDefinition> callbackDefs = this.callbackMethods.get(sc);
				if (callbackDefs != null)
					for (CallbackDefinition cd : callbackDefs)
						callbackMethodSigs.put(sc, cd.getTargetMethod());
			}
		}
		entryPointCreator.setCallbackFunctions(callbackMethodSigs);
		entryPointCreator.setFragments(fragmentClasses);
		return entryPointCreator;
	}

	/**
	 * Gets the components to analyze. If the given component is not null, we assume that only this component and the
	 * application class (if any) shall be analyzed. Otherwise, all components are to be analyzed.
	 *
	 * @param component
	 *            A component class name to only analyze this class and the application class (if any), or null to
	 *            analyze all classes.
	 * @return The set of classes to analyze
	 */
	private Set<SootClass> getComponentsToAnalyze(SootClass component) {
		if (component == null)
			return this.entrypoints;
		else {
			// We always analyze the application class together with each
			// component as there might be interactions between the two
			Set<SootClass> components = new HashSet<>(2);
			components.add(component);

			String applictionName = manifest.getApplicationName();
			if (applictionName != null && !applictionName.isEmpty())
				components.add(Scene.v().getSootClassUnsafe(applictionName));
			return components;
		}
	}

	/**
	 * Gets the dummy main method that was used for the last callgraph construction. You need to run a data flow
	 * analysis or call constructCallgraph() first, otherwise you will get a null result.
	 *
	 * @return The dummy main method
	 */
	public SootMethod getDummyMainMethod() {
		return dummyMainMethod;
	}

	public void setIcfgFactory(BiDirICFGFactory factory) {
		this.cfgFactory = factory;
	}

	public InfoflowAndroidConfiguration getConfig() {
		return this.config;
	}

	public void setConfig(InfoflowAndroidConfiguration config) {
		this.config = config;
	}

	public void setCallbackFile(String callbackFile) {
		this.callbackFile = callbackFile;
	}

}
