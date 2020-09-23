package com.linkedin.dagli.fasttext.anonymized;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class Args {

	public enum model_name {
		cbow(1), sg(2), sup(3);

		private int value;

		private model_name(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public static model_name fromValue(int value) throws IllegalArgumentException {
			try {
				value -= 1;
				return model_name.values()[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("Unknown model_name enum value :" + value);
			}
		}
	}

	public enum loss_name {
		hs(1), ns(2), softmax(3);
		private int value;

		private loss_name(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public static loss_name fromValue(int value) throws IllegalArgumentException {
			try {
				value -= 1;
				return loss_name.values()[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("Unknown loss_name enum value :" + value);
			}
		}
	}

	public String input;
	public String output;
	public String test;
	public double lr = 0.05;
	public int lrUpdateRate = 100;
	public int dim = 100;
	public int ws = 5;
	public int epoch = 5;
	public int minCount = 5;
	public int minCountLabel = 0;
	public int neg = 5;
	public int wordNgrams = 1;
	public loss_name loss = loss_name.ns;
	public model_name model = model_name.sg;
	public int bucket = 2000000;
	public int minn = 3;
	public int maxn = 6;
	public int thread = 1;
	public double t = 1e-4;
	public String label = "__label__";
	public int verbose = 2;
	public String pretrainedVectors = "";

	public int hashBits = 63; // must not be greater than 63

	public void printHelp() {
		System.out.println("\n" + "The following arguments are mandatory:\n"
				+ "  -input              training file path\n"
				+ "  -output             output file path\n\n"
				+ "The following arguments are optional:\n"
				+ "  -lr                 learning rate [" + lr + "]\n"
				+ "  -lrUpdateRate       change the rate of updates for the learning rate [" + lrUpdateRate + "]\n"
				+ "  -dim                size of word vectors [" + dim + "]\n"
				+ "  -ws                 size of the context window [" + ws + "]\n"
				+ "  -epoch              number of epochs [" + epoch + "]\n"
				+ "  -minCount           minimal number of word occurences [" + minCount + "]\n"
				+ "  -minCountLabel      minimal number of label occurences [" + minCountLabel + "]\n"
				+ "  -neg                number of negatives sampled [" + neg + "]\n"
				+ "  -wordNgrams         max length of word ngram [" + wordNgrams + "]\n"
				+ "  -loss               loss function {ns, hs, softmax} [ns]\n"
				+ "  -bucket             number of buckets [" + bucket + "]\n"
				+ "  -minn               min length of char ngram [" + minn + "]\n"
				+ "  -maxn               max length of char ngram [" + maxn + "]\n"
				+ "  -thread             number of threads [" + thread + "]\n"
				+ "  -t                  sampling threshold [" + t + "]\n"
				+ "  -label              labels prefix [" + label + "]\n"
				+ "  -verbose            verbosity level [" + verbose + "]\n"
				+ "  -pretrainedVectors  pretrained word vectors for supervised learning []");
	}

	public void save(OutputStream ofs) throws IOException {
		IOUtil ioutil = new IOUtil();
		ofs.write(ioutil.intToByteArray(dim));
		ofs.write(ioutil.intToByteArray(ws));
		ofs.write(ioutil.intToByteArray(epoch));
		ofs.write(ioutil.intToByteArray(minCount));
		ofs.write(ioutil.intToByteArray(neg));
		ofs.write(ioutil.intToByteArray(wordNgrams));
		ofs.write(ioutil.intToByteArray(loss.value));
		ofs.write(ioutil.intToByteArray(model.value));
		ofs.write(ioutil.intToByteArray(bucket));
		ofs.write(ioutil.intToByteArray(minn));
		ofs.write(ioutil.intToByteArray(maxn));
		ofs.write(ioutil.intToByteArray(lrUpdateRate));
		ofs.write(ioutil.doubleToByteArray(t));
	}

	public void load(InputStream input) throws IOException {
		IOUtil ioutil = new IOUtil();
		dim = ioutil.readInt(input);
		ws = ioutil.readInt(input);
		epoch = ioutil.readInt(input);
		minCount = ioutil.readInt(input);
		neg = ioutil.readInt(input);
		wordNgrams = ioutil.readInt(input);
		loss = loss_name.fromValue(ioutil.readInt(input));
		model = model_name.fromValue(ioutil.readInt(input));
		bucket = ioutil.readInt(input);
		minn = ioutil.readInt(input);
		maxn = ioutil.readInt(input);
		lrUpdateRate = ioutil.readInt(input);
		t = ioutil.readDouble(input);
	}

	public void parseArgs(String[] args) {
		String command = args[0];
		if ("supervised".equalsIgnoreCase(command)) {
			model = model_name.sup;
			loss = loss_name.softmax;
			minCount = 1;
			minn = 0;
			maxn = 0;
			lr = 0.1;
		} else if ("cbow".equalsIgnoreCase(command)) {
			model = model_name.cbow;
		}
		int ai = 1;
		while (ai < args.length) {
			if (args[ai].charAt(0) != '-') {
				throw new IllegalArgumentException("Provided argument without a dash!");
			} else if ("-input".equals(args[ai])) {
				input = args[ai + 1];
			} else if ("-test".equals(args[ai])) {
				test = args[ai + 1];
			} else if ("-output".equals(args[ai])) {
				output = args[ai + 1];
			} else if ("-lr".equals(args[ai])) {
				lr = Double.parseDouble(args[ai + 1]);
			} else if ("-lrUpdateRate".equals(args[ai])) {
				lrUpdateRate = Integer.parseInt(args[ai + 1]);
			} else if ("-dim".equals(args[ai])) {
				dim = Integer.parseInt(args[ai + 1]);
			} else if ("-ws".equals(args[ai])) {
				ws = Integer.parseInt(args[ai + 1]);
			} else if ("-epoch".equals(args[ai])) {
				epoch = Integer.parseInt(args[ai + 1]);
			} else if ("-minCount".equals(args[ai])) {
				minCount = Integer.parseInt(args[ai + 1]);
			} else if ("-minCountLabel".equals(args[ai])) {
				minCountLabel = Integer.parseInt(args[ai + 1]);
			} else if ("-neg".equals(args[ai])) {
				neg = Integer.parseInt(args[ai + 1]);
			} else if ("-wordNgrams".equals(args[ai])) {
				wordNgrams = Integer.parseInt(args[ai + 1]);
			} else if ("-loss".equals(args[ai])) {
				if ("hs".equalsIgnoreCase(args[ai + 1])) {
					loss = loss_name.hs;
				} else if ("ns".equalsIgnoreCase(args[ai + 1])) {
					loss = loss_name.ns;
				} else if ("softmax".equalsIgnoreCase(args[ai + 1])) {
					loss = loss_name.softmax;
				} else {
					throw new IllegalArgumentException("Unknown loss: " + args[ai + 1]);
				}
			} else if ("-bucket".equals(args[ai])) {
				bucket = Integer.parseInt(args[ai + 1]);
			} else if ("-minn".equals(args[ai])) {
				minn = Integer.parseInt(args[ai + 1]);
			} else if ("-maxn".equals(args[ai])) {
				maxn = Integer.parseInt(args[ai + 1]);
			} else if ("-thread".equals(args[ai])) {
				thread = Integer.parseInt(args[ai + 1]);
			} else if ("-t".equals(args[ai])) {
				t = Double.parseDouble(args[ai + 1]);
			} else if ("-label".equals(args[ai])) {
				label = args[ai + 1];
			} else if ("-verbose".equals(args[ai])) {
				verbose = Integer.parseInt(args[ai + 1]);
			} else if ("-pretrainedVectors".equals(args[ai])) {
				pretrainedVectors = args[ai + 1];
			} else {
				throw new IllegalArgumentException("Unknown argument: " + args[ai]);
			}
			ai += 2;
		}
		if (Utils.isEmpty(input) /* || Utils.isEmpty(output) */) {
			throw new IllegalArgumentException("Empty input path.");
		}
		if (wordNgrams <= 1 && maxn == 0) {
			bucket = 0;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Args [input=");
		builder.append(input);
		builder.append(", output=");
		builder.append(output);
		builder.append(", test=");
		builder.append(test);
		builder.append(", lr=");
		builder.append(lr);
		builder.append(", lrUpdateRate=");
		builder.append(lrUpdateRate);
		builder.append(", dim=");
		builder.append(dim);
		builder.append(", ws=");
		builder.append(ws);
		builder.append(", epoch=");
		builder.append(epoch);
		builder.append(", minCount=");
		builder.append(minCount);
		builder.append(", minCountLabel=");
		builder.append(minCountLabel);
		builder.append(", neg=");
		builder.append(neg);
		builder.append(", wordNgrams=");
		builder.append(wordNgrams);
		builder.append(", loss=");
		builder.append(loss);
		builder.append(", model=");
		builder.append(model);
		builder.append(", bucket=");
		builder.append(bucket);
		builder.append(", minn=");
		builder.append(minn);
		builder.append(", maxn=");
		builder.append(maxn);
		builder.append(", thread=");
		builder.append(thread);
		builder.append(", t=");
		builder.append(t);
		builder.append(", label=");
		builder.append(label);
		builder.append(", verbose=");
		builder.append(verbose);
		builder.append(", pretrainedVectors=");
		builder.append(pretrainedVectors);
		builder.append("]");
		return builder.toString();
	}

}
