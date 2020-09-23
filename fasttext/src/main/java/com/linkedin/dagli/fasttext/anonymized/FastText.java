package com.linkedin.dagli.fasttext.anonymized;

import com.linkedin.dagli.fasttext.anonymized.io.BufferedLineReader;
import com.linkedin.dagli.fasttext.anonymized.io.LineReader;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.embedding.classification.FastTextInternal;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;


/**
 * FastText class, can be used as a lib in other projects
 *
 * @author Ivan
 *
 */
public class FastText {

	private Args args_;
	private Dictionary dict_;
	private Matrix input_;
	private Matrix output_;
	private Model model_;

	private AtomicLong tokenCount_;
	private long start_;

	private String charsetName_ = "UTF-8";
	private Class<? extends LineReader> lineReaderClass_ = BufferedLineReader.class;

	public void printInfo(float progress, float loss) {
		float t = (float) (System.currentTimeMillis() - start_) / 1000;
		float ws = (float) (tokenCount_.get()) / t;
		float wst = (float) (tokenCount_.get()) / t / args_.thread;
		float lr = (float) (args_.lr * (1.0f - progress));
		int eta = (int) (t / progress * (1 - progress));
		int etah = eta / 3600;
		int etam = (eta - etah * 3600) / 60;
		System.out.printf("\rProgress: %.1f%% words/sec: %d words/sec/thread: %d lr: %.6f loss: %.6f eta: %d h %d m",
				100 * progress, (int) ws, (int) wst, lr, loss, etah, etam);
	}

	public void supervised(Model model, float lr, final IntArrayList wordRowIDs, final IntArrayList ngramRowIDs, final IntArrayList labelRowIDs) {
		if (labelRowIDs.isEmpty() || wordRowIDs.isEmpty()) {
			return;
		}
		int i = Utils.randomInt(model.rng, 1, labelRowIDs.size()) - 1;
		model.update(wordRowIDs, ngramRowIDs, labelRowIDs.getInt(i), lr);
	}

	public class TrainThread extends Thread {
		final FastText _fastText;
		int _threadId;
		final CyclicBarrier _startBarrier;

		public TrainThread(FastText ft, int threadId, CyclicBarrier startBarrier) {
			super("FT-TrainThread-" + threadId);
			_fastText = ft;
			_threadId = threadId;
			_startBarrier = startBarrier;
		}

		public void run() {
			if (args_.verbose > 2) {
				System.out.println("thread: " + _threadId + " RUNNING!");
			}
			Exception catchedException = null;
			LineReader lineReader = null;
			try {
				lineReader = lineReaderClass_.getConstructor(String.class, String.class).newInstance(args_.input,
						charsetName_);
				lineReader.skipLine(_threadId * threadFileSize / args_.thread);

				Model model = new Model(input_, output_, args_, _threadId);
				if (args_.model == Args.model_name.sup) {
					model.setTargetCounts(dict_.getLabelCounts());
				} else {
					throw new IllegalArgumentException();
				}

				final long ntokens = dict_.getTotalTokensRead();
				long localTokenCount = 0;

				LongArrayList wordHashes = new LongArrayList(16);
				IntArrayList wordIDs = new IntArrayList(16);
				IntArrayList labelIDs = new IntArrayList(16);

				// wait for everyone to get set up before training commences
				if (_startBarrier != null) {
					_startBarrier.await();
				}

				String[] lineTokens;
				while (tokenCount_.get() < args_.epoch * ntokens) {
					lineTokens = lineReader.readLineTokens();
					if (lineTokens == null) {
						try {
							lineReader.rewind();
							if (args_.verbose > 2) {
								System.out.println("Input file reloaded!");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						lineTokens = lineReader.readLineTokens();
					}

					float progress = (float) (tokenCount_.get()) / (args_.epoch * ntokens);
					float lr = (float) (args_.lr * (1.0 - progress));
					if (lr <= 0) { // possible for tokenCount to exceed epochs * tokens yielding progress > 1.0
						break; // unlearning things is bad
					}

					localTokenCount += dict_.getLine(lineTokens, wordHashes, wordIDs, labelIDs);
					if (labelIDs.isEmpty() || wordIDs.isEmpty()) {
						continue;
					}

					IntArrayList ngramRowIDs = dict_.getNgramRowIDs(wordHashes, args_.wordNgrams, dict_.distinctWordCount(), args_.bucket);
					supervised(model, lr, wordIDs, ngramRowIDs, labelIDs);

					if (localTokenCount > args_.lrUpdateRate) {
						tokenCount_.addAndGet(localTokenCount);
						localTokenCount = 0;
						if (_threadId == 0 && args_.verbose > 1 && (System.currentTimeMillis() - start_) % 1000 == 0) {
							printInfo(progress, model.getLoss());
						}
					}
				}

				if (_threadId == 0 && args_.verbose > 1) {
					printInfo(1.0f, model.getLoss());
				}
			} catch (Exception e) {
				catchedException = e;
			} finally {
				if (lineReader != null)
					try {
						lineReader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}

			// exit from thread
			synchronized (_fastText) {
				if (args_.verbose > 2) {
					System.out.println("\nthread: " + _threadId + " EXIT!");
				}
				_fastText.threadCount--;
				_fastText.notify();
				if (catchedException != null) {
					throw new RuntimeException(catchedException);
				}
			}
		}
	}

	public void loadVectors(String filename) throws IOException {
		List<String> words;
		Matrix mat; // temp. matrix for pretrained vectors
		int n, dim;

		BufferedReader dis = null;
		String line;
		String[] lineParts;
		try {
			dis = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));

			line = dis.readLine();
			lineParts = line.split(" ");
			n = Integer.parseInt(lineParts[0]);
			dim = Integer.parseInt(lineParts[1]);

			words = new ArrayList<String>(n);

			if (dim != args_.dim) {
				throw new IllegalArgumentException(
						"Dimension of pretrained vectors does not match args -dim option, pretrain dim is " + dim
								+ ", args dim is " + args_.dim);
			}

			mat = new Matrix(n, dim);
			for (int i = 0; i < n; i++) {
				line = dis.readLine();
				lineParts = line.split(" ");
				String word = lineParts[0];
				for (int j = 1; j <= dim; j++) {
					mat.data_[i][j - 1] = Float.parseFloat(lineParts[j]);
				}
				words.add(word);
				dict_.addWord(word);
			}

			//dict_.threshold(1, 0);
			input_ = new Matrix(dict_.distinctWordCount() + args_.bucket, args_.dim);
			input_.uniform(1.0f / args_.dim);
			for (int i = 0; i < n; i++) {
				int idx = dict_.getWordID(words.get(i));
				if (idx < 0 || idx >= dict_.distinctWordCount())
					continue;
				for (int j = 0; j < dim; j++) {
					input_.data_[idx][j] = mat.data_[i][j];
				}
			}

		} catch (IOException e) {
			throw new IOException("Pretrained vectors file cannot be opened!", e);
		} finally {
			try {
				if (dis != null) {
					dis.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public int embeddingRowIndexForWordID(int wordID) {
		return wordID;
	}

	public static int embeddingRowIndexForNgramHash(long hash, int distinctWordCount, int bucketCount) {
		return (int) ((Math.abs(hash) % bucketCount) + distinctWordCount);
	}

	int threadCount;
	long threadFileSize;

	public FastTextInternal.Model<String> train(FastTextOptions options)
			throws IOException, InterruptedException {
		args_ = options.getArgs();
		dict_ = new Dictionary(args_);
		dict_.setCharsetName(charsetName_);
		dict_.setLineReaderClass(lineReaderClass_);

		if ("-".equals(args_.input)) {
			throw new IOException("Cannot use stdin for training!");
		}

		File file = new File(args_.input);
		if (!(file.exists() && file.isFile() && file.canRead())) {
			throw new IOException("Input file cannot be opened! " + args_.input);
		}

		if (args_.verbose > 0) {
			System.err.println("Reading " + options.getExampleCount() + " examples from file " + args_.input);
			if (options.getSynchronizedStart()) {
				System.err.println("Synchronized start has been selected.  There will be a delay after the first pass over "
						+ "the data while the threads find their start positions in the input data file.");
			}
		}
		dict_.readFromFile(args_.input);
		threadFileSize = options.getExampleCount(); // previously Utils.sizeLine(args_.input);

		if (!Utils.isEmpty(args_.pretrainedVectors)) {
			loadVectors(args_.pretrainedVectors);
		} else {
			input_ = new Matrix(dict_.distinctWordCount() + args_.bucket, args_.dim); //new Matrix(dict_.nwords() + args_.bucket, args_.dim);
			input_.uniform(1.0f / args_.dim);
		}

		if (args_.model == Args.model_name.sup) {
			output_ = new Matrix(dict_.distinctLabelCount(), args_.dim);
		} else {
			throw new IllegalArgumentException();
			//output_ = new Matrix(dict_.nwords(), args_.dim);
		}
		output_.zero();

		start_ = System.currentTimeMillis();
		tokenCount_ = new AtomicLong(0);
		long t0 = System.currentTimeMillis();
		threadCount = args_.thread;
		List<Thread> threads = new ArrayList<>(args_.thread);
		final CyclicBarrier startingBarrier = options.getSynchronizedStart() ? new CyclicBarrier(args_.thread) : null;
		for (int i = 0; i < args_.thread; i++) {
			Thread t = new TrainThread(this, i, startingBarrier);
			t.setUncaughtExceptionHandler(trainThreadExceptionHandler);
			t.start();
			threads.add(t);
		}

		for (Thread t : threads) {
			t.join();
		}

		if (threadCount != 0) {
			throw new RuntimeException("Not all training threads completed successfully");
		}

		model_ = new Model(input_, output_, args_, 0);

		if (args_.verbose > 1) {
			long trainTime = (System.currentTimeMillis() - t0) / 1000;
			System.out.printf("\nTrain time used: %d sec\n", trainTime);
		}

		if (args_.model != Args.model_name.sup) {
			throw new IllegalArgumentException();
		}

		Long2ObjectOpenHashMap<DenseFloatArrayVector> wordEmbeddings = new Long2ObjectOpenHashMap<>(dict_.distinctWordCount());

		dict_.getWordIDMap()
				.long2IntEntrySet()
				.fastForEach(wordToID -> wordEmbeddings.put(wordToID.getLongKey(),
						DenseFloatArrayVector.wrap(model_.getInputEmbedding(embeddingRowIndexForWordID(wordToID.getIntValue())))));

		return new FastTextInternal.Model<>(dict_.getLabels(), Arrays.stream(model_.getLabelEmbeddings())
				.map(labelEmbedding -> DenseFloatArrayVector.wrap(labelEmbedding))
				.toArray(DenseFloatArrayVector[]::new), wordEmbeddings,
				Arrays.stream(model_.getInputEmbeddingsStartingAtRow(dict_.distinctWordCount()))
						.map(embedding -> DenseFloatArrayVector.wrap(embedding))
						.toArray(DenseFloatArrayVector[]::new), options.getMultilabel(), args_.wordNgrams);
	}

	protected Thread.UncaughtExceptionHandler trainThreadExceptionHandler = new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread th, Throwable ex) {
			ex.printStackTrace();
		}
	};

	public Args getArgs() {
		return args_;
	}

	public Dictionary getDict() {
		return dict_;
	}

	public String getLabel(int id) {
		return dict_.getLabel(id);
	}

	public Matrix getInput() {
		return input_;
	}

	public Matrix getOutput() {
		return output_;
	}

	public Model getModel() {
		return model_;
	}

	public void setArgs(Args args) {
		this.args_ = args;
	}

	public void setDict(Dictionary dict) {
		this.dict_ = dict;
	}

	public void setInput(Matrix input) {
		this.input_ = input;
	}

	public void setOutput(Matrix output) {
		this.output_ = output;
	}

	public void setModel(Model model) {
		this.model_ = model;
	}

	public String getCharsetName() {
		return charsetName_;
	}

	public Class<? extends LineReader> getLineReaderClass() {
		return lineReaderClass_;
	}

	public void setCharsetName(String charsetName) {
		this.charsetName_ = charsetName;
	}

	public void setLineReaderClass(Class<? extends LineReader> lineReaderClass) {
		this.lineReaderClass_ = lineReaderClass;
	}

}
